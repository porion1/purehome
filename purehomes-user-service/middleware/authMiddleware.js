const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const User = require('../models/userModel');

/**
 * INNOVATION ALGORITHM: Token Reputation Scoring with Adaptive Blacklisting (TRS-AB)
 *
 * This algorithm dynamically assesses token trustworthiness based on:
 * 1. Request fingerprinting (IP, User-Agent)
 * 2. Behavioral anomaly detection (unusual request patterns)
 * 3. Token age and usage frequency scoring
 * 4. Adaptive sliding window for anomaly thresholds
 */
class TokenReputationManager {
    constructor() {
        this.tokenRegistry = new Map();
        this.blacklistedTokens = new Set();
        this.anomalyThresholds = {
            maxRequestsPerMinute: 60,
            maxGeoChangesPerHour: 3,
            reputationThreshold: 0.3
        };

        // Cleanup every hour
        setInterval(() => this.cleanupExpiredTokens(), 3600000);
    }

    generateFingerprint(req) {
        const components = [
            req.ip || req.connection.remoteAddress,
            req.headers['user-agent'] || 'unknown',
            req.headers['accept-language'] || 'unknown'
        ];

        return crypto.createHash('sha256').update(components.join('|')).digest('hex');
    }

    calculateGeoScore(req, tokenHistory) {
        if (!tokenHistory || tokenHistory.length === 0) return 1.0;

        const currentIp = req.ip || req.connection.remoteAddress;
        const previousIp = tokenHistory[tokenHistory.length - 1].ip;

        if (currentIp === previousIp) return 1.0;
        return 0.7;
    }

    calculateBehaviorScore(req, tokenHistory) {
        if (!tokenHistory || tokenHistory.length < 5) return 1.0;

        const now = Date.now();
        const lastMinute = tokenHistory.filter(h => now - h.timestamp < 60000);
        const lastHour = tokenHistory.filter(h => now - h.timestamp < 3600000);

        let score = 1.0;

        if (lastMinute.length > this.anomalyThresholds.maxRequestsPerMinute) {
            score -= 0.3;
        }

        const averageRate = lastHour.length / 60;
        const currentRate = lastMinute.length;
        if (currentRate > averageRate * 3) {
            score -= 0.2;
        }

        return Math.max(0, score);
    }

    updateReputation(tokenId, req, isValidToken) {
        const now = Date.now();
        const tokenHash = crypto.createHash('sha256').update(tokenId).digest('hex');

        if (!this.tokenRegistry.has(tokenHash)) {
            this.tokenRegistry.set(tokenHash, {
                history: [],
                reputationScore: 1.0,
                firstSeen: now,
                lastSeen: now,
                anomalyCount: 0
            });
        }

        const record = this.tokenRegistry.get(tokenHash);

        record.history.push({
            timestamp: now,
            ip: req.ip || req.connection.remoteAddress,
            userAgent: req.headers['user-agent'],
            fingerprint: this.generateFingerprint(req),
            isValid: isValidToken
        });

        if (record.history.length > 1000) {
            record.history.shift();
        }

        if (isValidToken) {
            const geoScore = this.calculateGeoScore(req, record.history);
            const behaviorScore = this.calculateBehaviorScore(req, record.history);

            record.reputationScore = (geoScore * 0.4) + (behaviorScore * 0.6);

            const ageHours = (now - record.firstSeen) / 3600000;
            record.reputationScore *= Math.max(0.5, 1 - (ageHours / 720));
        } else {
            record.reputationScore *= 0.5;
            record.anomalyCount++;
        }

        record.lastSeen = now;

        if (record.reputationScore < this.anomalyThresholds.reputationThreshold) {
            this.blacklistedTokens.add(tokenHash);
        }

        return record.reputationScore;
    }

    isBlacklisted(tokenId) {
        const tokenHash = crypto.createHash('sha256').update(tokenId).digest('hex');
        return this.blacklistedTokens.has(tokenHash);
    }

    getReputationScore(tokenId) {
        const tokenHash = crypto.createHash('sha256').update(tokenId).digest('hex');
        const record = this.tokenRegistry.get(tokenHash);
        return record ? record.reputationScore : 1.0;
    }

    blacklistToken(tokenId) {
        const tokenHash = crypto.createHash('sha256').update(tokenId).digest('hex');
        this.blacklistedTokens.add(tokenHash);
    }

    cleanupExpiredTokens() {
        const sevenDaysAgo = Date.now() - 7 * 24 * 3600000;

        for (const [hash, record] of this.tokenRegistry.entries()) {
            if (record.lastSeen < sevenDaysAgo) {
                this.tokenRegistry.delete(hash);
                this.blacklistedTokens.delete(hash);
            }
        }
    }

    getMetrics() {
        return {
            activeTokens: this.tokenRegistry.size,
            blacklistedTokens: this.blacklistedTokens.size,
            averageReputation: Array.from(this.tokenRegistry.values())
                .reduce((sum, r) => sum + r.reputationScore, 0) / (this.tokenRegistry.size || 1)
        };
    }
}

class JWTTokenManager {
    constructor() {
        this.tokenTypes = {
            ACCESS: 'access',
            REFRESH: 'refresh'
        };
    }

    generateAccessToken(userId, deviceInfo = {}) {
        const payload = {
            userId,
            type: this.tokenTypes.ACCESS,
            fingerprint: crypto.createHash('sha256')
                .update(`${deviceInfo.ip || ''}|${deviceInfo.userAgent || ''}`)
                .digest('hex'),
            jti: crypto.randomBytes(16).toString('hex'),
            iat: Math.floor(Date.now() / 1000)
        };

        const expiresIn = process.env.JWT_ACCESS_EXPIRY || '15m';
        return jwt.sign(payload, process.env.JWT_SECRET, { expiresIn });
    }

    generateRefreshToken(userId, deviceInfo = {}) {
        const payload = {
            userId,
            type: this.tokenTypes.REFRESH,
            deviceId: crypto.createHash('sha256')
                .update(`${deviceInfo.ip}|${deviceInfo.userAgent}`)
                .digest('hex'),
            jti: crypto.randomBytes(16).toString('hex'),
            iat: Math.floor(Date.now() / 1000)
        };

        const expiresIn = process.env.JWT_REFRESH_EXPIRY || '7d';
        return jwt.sign(payload, process.env.JWT_REFRESH_SECRET || process.env.JWT_SECRET, { expiresIn });
    }

    verifyToken(token, secret, options = {}) {
        try {
            const decoded = jwt.verify(token, secret);

            if (options.expectedType && decoded.type !== options.expectedType) {
                throw new Error('Invalid token type');
            }

            return { valid: true, decoded };
        } catch (error) {
            return { valid: false, error: error.message };
        }
    }

    decodeToken(token) {
        return jwt.decode(token);
    }
}

const reputationManager = new TokenReputationManager();
const tokenManager = new JWTTokenManager();

const protect = async (req, res, next) => {
    let token;

    if (req.headers.authorization && req.headers.authorization.startsWith('Bearer')) {
        token = req.headers.authorization.split(' ')[1];
    }

    if (!token) {
        return res.status(401).json({
            error: 'Authentication required',
            code: 'NO_TOKEN'
        });
    }

    if (reputationManager.isBlacklisted(token)) {
        return res.status(401).json({
            error: 'Token revoked',
            code: 'TOKEN_BLACKLISTED'
        });
    }

    try {
        const verification = tokenManager.verifyToken(token, process.env.JWT_SECRET, {
            expectedType: 'access'
        });

        if (!verification.valid) {
            reputationManager.updateReputation(token, req, false);
            return res.status(401).json({
                error: 'Invalid token',
                code: 'INVALID_TOKEN'
            });
        }

        const decoded = verification.decoded;

        const tokenAgeMinutes = (Date.now() / 1000 - decoded.iat) / 60;
        const maxTokenAge = parseInt(process.env.JWT_MAX_AGE_MINUTES) || 720;

        if (tokenAgeMinutes > maxTokenAge) {
            return res.status(401).json({
                error: 'Token expired',
                code: 'TOKEN_EXPIRED'
            });
        }

        const user = await User.findById(decoded.userId).select('-password -refreshToken');

        if (!user) {
            reputationManager.blacklistToken(token);
            return res.status(401).json({
                error: 'User not found',
                code: 'USER_NOT_FOUND'
            });
        }

        if (user.isBanned || user.deletedAt) {
            reputationManager.blacklistToken(token);
            return res.status(403).json({
                error: 'Account inactive',
                code: 'ACCOUNT_INACTIVE'
            });
        }

        const reputationScore = reputationManager.updateReputation(token, req, true);

        if (reputationScore < 0.2) {
            return res.status(401).json({
                error: 'Suspicious activity detected',
                code: 'LOW_REPUTATION'
            });
        }

        // 🔥 INTEGRATION WITH TAP: Check for behavioral anomalies
        let anomalyResult = null;
        try {
            anomalyResult = await user.checkAnomaly(req);

            // Log anomaly for monitoring (only if significant)
            if (anomalyResult.score > 30) {
                console.log(`⚠️ TAP Anomaly [User: ${user._id}]:`, {
                    score: anomalyResult.score,
                    risk: anomalyResult.risk,
                    reasons: anomalyResult.reasons,
                    action: anomalyResult.action
                });
            }

            // 🔥 TAP: Block if critical anomaly
            if (anomalyResult.shouldBlock) {
                await user.updateSecurityFlags(anomalyResult);
                return res.status(401).json({
                    error: 'Suspicious activity detected',
                    code: 'SUSPICIOUS_ACTIVITY',
                    anomalyScore: anomalyResult.score,
                    risk: anomalyResult.risk,
                    action: anomalyResult.action,
                    timestamp: new Date().toISOString()
                });
            }

            // 🔥 TAP: Require additional verification for high risk
            if (anomalyResult.action === 'require_2fa') {
                return res.status(403).json({
                    error: 'Additional verification required',
                    code: 'VERIFICATION_REQUIRED',
                    anomalyScore: anomalyResult.score,
                    risk: anomalyResult.risk,
                    reasons: anomalyResult.reasons
                });
            }

        } catch (err) {
            // Silent fail - don't break auth if TAP has issues
            console.error('TAP error:', err.message);
        }

        // Combine reputation score with TAP anomaly score for comprehensive risk assessment
        const combinedRiskScore = Math.min(100, (reputationScore * 100) + (anomalyResult?.score || 0));

        req.user = user;
        req.tokenMetadata = {
            jti: decoded.jti,
            issuedAt: decoded.iat,
            reputationScore,
            tapAnomalyScore: anomalyResult?.score || 0,
            combinedRiskScore
        };

        next();
    } catch (error) {
        return res.status(401).json({
            error: 'Authentication failed',
            code: 'AUTH_ERROR'
        });
    }
};

const restrictTo = (...roles) => {
    return (req, res, next) => {
        if (!req.user) {
            return res.status(401).json({
                error: 'Authentication required',
                code: 'NO_USER'
            });
        }

        if (!roles.includes(req.user.role)) {
            return res.status(403).json({
                error: 'Access denied',
                code: 'INSUFFICIENT_PERMISSIONS'
            });
        }

        next();
    };
};

const logout = async (req, res) => {
    const token = req.headers.authorization?.split(' ')[1];

    if (token) {
        reputationManager.blacklistToken(token);
    }

    res.status(200).json({
        message: 'Logged out successfully',
        code: 'LOGOUT_SUCCESS'
    });
};

const getTokenMetrics = () => {
    return reputationManager.getMetrics();
};

// 🔥 New: Combined security metrics endpoint
const getSecurityMetrics = async (req, res) => {
    if (!req.user || req.user.role !== 'admin') {
        return res.status(403).json({ error: 'Access denied' });
    }

    const tapMetrics = await User.getAnomalyMetrics();

    res.json({
        trs_ab: reputationManager.getMetrics(),
        tap: tapMetrics,
        combined: {
            totalActiveTokens: reputationManager.activeTokens,
            totalAnomalyProfiles: tapMetrics.activeProfiles,
            learningMode: tapMetrics.learningMode
        }
    });
};

module.exports = {
    protect,
    restrictTo,
    logout,
    getTokenMetrics,
    getSecurityMetrics,
    JWTTokenManager,
    TokenReputationManager
};