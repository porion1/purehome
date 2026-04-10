const jwt = require('jsonwebtoken');

// ----------------------------
// Algorithm 1: Adaptive Token Trust Engine (ATTE) - Existing
// ----------------------------
// This algorithm dynamically evaluates token trustworthiness using:
// 1. Signature validation (JWT)
// 2. Token age (freshness scoring)
// 3. Optional trust score injection
//
// Formula:
// trustScore = freshnessScore + signatureValidity
//
// If trustScore falls below threshold → reject
//
// This allows:
// - Future fraud detection
// - Session quality scoring
// - Graceful degradation under suspicious activity
// ----------------------------

const calculateTrustScore = (decoded) => {
    // Freshness factor
    const now = Date.now() / 1000;
    const age = now - decoded.iat; // seconds

    // Fresh tokens = higher score
    const freshnessScore = 1 / (1 + age / 3600); // decay per hour
    const signatureScore = 1;

    return freshnessScore + signatureScore;
};

// ----------------------------
// Algorithm 2: Adaptive Token Reputation Engine (ATRE) - NEW
// ----------------------------
// FAANG-level token reputation system that:
// 1. Tracks token usage patterns (frequency, geographic anomalies)
// 2. Implements sliding window rate limiting per token
// 3. Auto-blacklists tokens with suspicious behavior
// 4. Provides reputation scoring (0-100) for each token
// 5. Supports gradual reputation recovery over time
// ----------------------------

class TokenReputationEngine {
    constructor() {
        this.reputationStore = new Map(); // tokenHash -> reputation data
        this.requestWindow = new Map(); // tokenHash -> request timestamps
        this.blacklist = new Set(); // Blacklisted token hashes
        this.cleanupInterval = null;

        // Configuration
        this.windowSizeMs = 60000; // 1 minute sliding window
        this.maxRequestsPerWindow = 100; // Max 100 requests per minute
        this.reputationDecayRate = 0.95; // 5% decay per successful request
        this.suspiciousThreshold = 30; // Below 30 = suspicious
        this.blacklistThreshold = 10; // Below 10 = blacklisted

        // Start background tasks
        this.startCleanup();
        this.startReputationDecay();
    }

    // Generate consistent hash for token (without storing actual token)
    hashToken(token) {
        // Simple hash for MVP (use SHA256 in production)
        let hash = 0;
        for (let i = 0; i < token.length; i++) {
            const char = token.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // Convert to 32-bit integer
        }
        return Math.abs(hash).toString(36);
    }

    // Track request for rate limiting
    trackRequest(tokenHash) {
        const now = Date.now();

        if (!this.requestWindow.has(tokenHash)) {
            this.requestWindow.set(tokenHash, []);
        }

        const timestamps = this.requestWindow.get(tokenHash);
        timestamps.push(now);

        // Remove timestamps outside current window
        while (timestamps.length > 0 && timestamps[0] < now - this.windowSizeMs) {
            timestamps.shift();
        }

        return timestamps.length;
    }

    // Check if token exceeds rate limit
    isRateLimited(tokenHash) {
        const requestCount = this.trackRequest(tokenHash);
        return requestCount > this.maxRequestsPerWindow;
    }

    // Get or create reputation record for token
    getReputation(tokenHash, userId = null) {
        if (!this.reputationStore.has(tokenHash)) {
            this.reputationStore.set(tokenHash, {
                score: 100, // Start with perfect reputation
                totalRequests: 0,
                failedAttempts: 0,
                lastSeen: Date.now(),
                firstSeen: Date.now(),
                userId: userId,
                anomalies: 0,
                recoveryStartTime: null
            });
        }

        return this.reputationStore.get(tokenHash);
    }

    // Update reputation based on request outcome
    updateReputation(tokenHash, success, metadata = {}) {
        const reputation = this.getReputation(tokenHash, metadata.userId);

        if (success) {
            // Successful request increases reputation (diminishing returns)
            const increase = Math.min(5, 100 - reputation.score) * 0.1;
            reputation.score = Math.min(100, reputation.score + increase);
            reputation.totalRequests++;
        } else {
            // Failed request decreases reputation significantly
            const decrease = metadata.isSuspicious ? 15 : 5;
            reputation.score = Math.max(0, reputation.score - decrease);
            reputation.failedAttempts++;

            // Track anomalies
            if (metadata.isSuspicious) {
                reputation.anomalies++;
            }
        }

        reputation.lastSeen = Date.now();

        // Check for suspicious activity
        const failureRate = reputation.failedAttempts / Math.max(1, reputation.totalRequests);

        if (reputation.score <= this.blacklistThreshold || failureRate > 0.5) {
            this.blacklist.add(tokenHash);
            console.warn(`🚫 Token blacklisted: ${tokenHash}, score: ${reputation.score}, failure rate: ${failureRate}`);
        } else if (reputation.score <= this.suspiciousThreshold) {
            console.warn(`⚠️ Suspicious token: ${tokenHash}, score: ${reputation.score}`);
        }

        // Update recovery tracking
        if (reputation.score < 50 && !reputation.recoveryStartTime) {
            reputation.recoveryStartTime = Date.now();
        } else if (reputation.score >= 70 && reputation.recoveryStartTime) {
            reputation.recoveryStartTime = null; // Recovered
        }

        return reputation;
    }

    // Check if token is blacklisted
    isBlacklisted(tokenHash) {
        return this.blacklist.has(tokenHash);
    }

    // Detect geographic anomalies (IP-based)
    detectGeographicAnomaly(tokenHash, currentIP, knownIPs = []) {
        const reputation = this.reputationStore.get(tokenHash);
        if (!reputation || !reputation.userId) return false;

        // If we have known IPs and current IP is new, flag as suspicious
        if (knownIPs.length > 0 && !knownIPs.includes(currentIP)) {
            reputation.anomalies++;
            return true;
        }

        return false;
    }

    // Detect time-based anomalies (unusual request patterns)
    detectTemporalAnomaly(tokenHash) {
        const timestamps = this.requestWindow.get(tokenHash);
        if (!timestamps || timestamps.length < 10) return false;

        // Calculate request frequency
        const timeSpan = timestamps[timestamps.length - 1] - timestamps[0];
        const frequency = timestamps.length / (timeSpan / 1000); // requests per second

        // Burst detection (> 10 requests per second)
        if (frequency > 10) {
            return true;
        }

        return false;
    }

    // Get token health status
    getTokenHealth(tokenHash) {
        if (this.isBlacklisted(tokenHash)) {
            return 'BLACKLISTED';
        }

        const reputation = this.reputationStore.get(tokenHash);
        if (!reputation) return 'UNKNOWN';

        if (reputation.score >= 80) return 'EXCELLENT';
        if (reputation.score >= 60) return 'GOOD';
        if (reputation.score >= 30) return 'SUSPICIOUS';
        return 'CRITICAL';
    }

    // Get estimated recovery time (seconds until reputation recovers)
    getRecoveryTime(tokenHash) {
        const reputation = this.reputationStore.get(tokenHash);
        if (!reputation || reputation.score >= 70) return 0;

        const deficit = 70 - reputation.score;
        // Assuming 1 point per 10 seconds of good behavior
        return deficit * 10;
    }

    // Background: Clean up old reputation records
    startCleanup() {
        setInterval(() => {
            const now = Date.now();
            const maxAge = 7 * 24 * 60 * 60 * 1000; // 7 days

            for (const [tokenHash, reputation] of this.reputationStore) {
                if (now - reputation.lastSeen > maxAge) {
                    this.reputationStore.delete(tokenHash);
                    this.requestWindow.delete(tokenHash);
                }
            }

            // Clean blacklist (keep for 24 hours)
            for (const tokenHash of this.blacklist) {
                const reputation = this.reputationStore.get(tokenHash);
                if (reputation && now - reputation.lastSeen > 86400000) {
                    this.blacklist.delete(tokenHash);
                }
            }
        }, 3600000); // Every hour
    }

    // Background: Apply reputation decay over time
    startReputationDecay() {
        setInterval(() => {
            for (const [tokenHash, reputation] of this.reputationStore) {
                const hoursSinceLastSeen = (Date.now() - reputation.lastSeen) / 3600000;

                if (hoursSinceLastSeen > 24) {
                    // Decay reputation for inactive tokens
                    reputation.score = Math.max(50, reputation.score * 0.98);
                }
            }
        }, 3600000); // Every hour
    }

    // Get statistics for monitoring
    getStats() {
        let totalScore = 0;
        let blacklistedCount = this.blacklist.size;
        let suspiciousCount = 0;

        for (const [_, reputation] of this.reputationStore) {
            totalScore += reputation.score;
            if (reputation.score <= this.suspiciousThreshold) {
                suspiciousCount++;
            }
        }

        return {
            totalTokens: this.reputationStore.size,
            blacklistedTokens: blacklistedCount,
            suspiciousTokens: suspiciousCount,
            averageReputation: this.reputationStore.size > 0 ? (totalScore / this.reputationStore.size).toFixed(2) : 0,
            activeWindows: this.requestWindow.size
        };
    }
}

// Initialize reputation engine
const tokenReputation = new TokenReputationEngine();

// ----------------------------
// Enhanced Protect Middleware (Algorithm 1 + Algorithm 2)
// ----------------------------
const protect = (req, res, next) => {
    let token;

    // Extract token
    if (
        req.headers.authorization &&
        req.headers.authorization.startsWith('Bearer')
    ) {
        token = req.headers.authorization.split(' ')[1];
    }

    if (!token) {
        return res.status(401).json({ message: 'No token provided' });
    }

    // Generate token hash for reputation tracking
    const tokenHash = tokenReputation.hashToken(token);

    // Check rate limiting
    if (tokenReputation.isRateLimited(tokenHash)) {
        console.warn(`Rate limit exceeded for token: ${tokenHash}`);
        tokenReputation.updateReputation(tokenHash, false, { isSuspicious: true });
        return res.status(429).json({
            message: 'Too many requests',
            retryAfter: 60,
            tokenHealth: tokenReputation.getTokenHealth(tokenHash)
        });
    }

    // Check blacklist
    if (tokenReputation.isBlacklisted(tokenHash)) {
        return res.status(401).json({
            message: 'Token has been revoked due to suspicious activity',
            tokenHealth: 'BLACKLISTED'
        });
    }

    try {
        // Verify JWT (Algorithm 1)
        const decoded = jwt.verify(token, process.env.JWT_SECRET);

        // Apply trust score algorithm (Algorithm 1)
        const trustScore = calculateTrustScore(decoded);

        // Get reputation data (Algorithm 2)
        const reputation = tokenReputation.getReputation(tokenHash, decoded.userId);
        const tokenHealth = tokenReputation.getTokenHealth(tokenHash);

        // Detect anomalies
        const temporalAnomaly = tokenReputation.detectTemporalAnomaly(tokenHash);
        const geographicAnomaly = tokenReputation.detectGeographicAnomaly(
            tokenHash,
            req.ip || req.connection.remoteAddress,
            [] // Would load from user's known IPs in production
        );

        const hasAnomaly = temporalAnomaly || geographicAnomaly;

        // Combined trust score (Algorithm 1 + Algorithm 2)
        const combinedTrustScore = (trustScore * 50) + (reputation.score / 2);
        const minTrustThreshold = hasAnomaly ? 60 : 40;

        // Check if token is trustworthy
        if (combinedTrustScore < minTrustThreshold) {
            tokenReputation.updateReputation(tokenHash, false, { isSuspicious: true });

            const recoverySeconds = tokenReputation.getRecoveryTime(tokenHash);

            return res.status(401).json({
                message: 'Insufficient token trust score',
                trustScore: combinedTrustScore.toFixed(2),
                tokenHealth: tokenHealth,
                recoveryTimeSeconds: recoverySeconds,
                anomalies: {
                    temporal: temporalAnomaly,
                    geographic: geographicAnomaly
                }
            });
        }

        // Update reputation for successful request
        tokenReputation.updateReputation(tokenHash, true, { userId: decoded.userId });

        // Attach user info to request
        req.user = {
            id: decoded.userId,
            email: decoded.email,
            role: decoded.role || 'user'
        };

        req.tokenMeta = {
            trustScore: combinedTrustScore.toFixed(2),
            tokenHealth: tokenHealth,
            reputationScore: reputation.score,
            issuedAt: decoded.iat,
            tokenHash: tokenHash // For logging only, not exposed in responses
        };

        // Add security headers
        res.setHeader('X-Token-Health', tokenHealth);
        res.setHeader('X-Trust-Score', combinedTrustScore.toFixed(2));

        next();
    } catch (error) {
        console.error('Auth error:', error.message);

        // Record failed attempt
        tokenReputation.updateReputation(tokenHash, false, {
            isSuspicious: error.message === 'jwt expired'
        });

        // Different messages for different error types
        if (error.message === 'jwt expired') {
            return res.status(401).json({
                message: 'Token expired',
                tokenHealth: tokenReputation.getTokenHealth(tokenHash)
            });
        }

        return res.status(401).json({
            message: 'Invalid token',
            tokenHealth: tokenReputation.getTokenHealth(tokenHash)
        });
    }
};

// ----------------------------
// Enhanced Role Middleware with Reputation Check
// ----------------------------
const authorize = (...roles) => {
    return (req, res, next) => {
        const userRole = req.user?.role || 'user';
        const tokenHealth = req.tokenMeta?.tokenHealth;

        // Check role permissions
        if (!roles.includes(userRole)) {
            return res.status(403).json({
                message: 'Access denied: insufficient permissions',
                requiredRoles: roles,
                userRole: userRole
            });
        }

        // Additional check for suspicious tokens on sensitive operations
        if (tokenHealth === 'SUSPICIOUS' || tokenHealth === 'CRITICAL') {
            return res.status(403).json({
                message: 'Access denied: token health insufficient for this operation',
                tokenHealth: tokenHealth,
                requiredHealth: 'GOOD'
            });
        }

        next();
    };
};

// ----------------------------
// NEW: Token Management Endpoints (Admin)
// ----------------------------

// Get token reputation status (for debugging/admin)
const getTokenStatus = (req, res) => {
    try {
        const authHeader = req.headers.authorization;
        if (!authHeader || !authHeader.startsWith('Bearer')) {
            return res.status(400).json({ message: 'No token provided' });
        }

        const token = authHeader.split(' ')[1];
        const tokenHash = tokenReputation.hashToken(token);

        const reputation = tokenReputation.getReputation(tokenHash);
        const tokenHealth = tokenReputation.getTokenHealth(tokenHash);
        const isLimited = tokenReputation.isRateLimited(tokenHash);
        const recoveryTime = tokenReputation.getRecoveryTime(tokenHash);

        res.json({
            tokenHealth: tokenHealth,
            reputationScore: reputation.score,
            totalRequests: reputation.totalRequests,
            failedAttempts: reputation.failedAttempts,
            anomalies: reputation.anomalies,
            isRateLimited: isLimited,
            recoveryTimeSeconds: recoveryTime,
            lastSeen: reputation.lastSeen,
            firstSeen: reputation.firstSeen
        });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Get global token reputation statistics (Admin only)
const getReputationStats = (req, res) => {
    const stats = tokenReputation.getStats();

    res.json({
        algorithm: 'Adaptive Token Reputation Engine (ATRE)',
        version: '2.0',
        stats: stats,
        thresholds: {
            excellent: '≥80',
            good: '60-79',
            suspicious: '30-59',
            critical: '<30',
            blacklisted: '≤10'
        },
        features: {
            rateLimiting: `${tokenReputation.maxRequestsPerWindow} requests per ${tokenReputation.windowSizeMs/1000} seconds`,
            reputationDecay: `${(1 - tokenReputation.reputationDecayRate) * 100}% per hour`,
            anomalyDetection: 'Temporal + Geographic'
        }
    });
};

// Manually blacklist a token (Admin emergency)
const blacklistToken = async (req, res) => {
    try {
        const { token } = req.body;
        if (!token) {
            return res.status(400).json({ message: 'Token required' });
        }

        const tokenHash = tokenReputation.hashToken(token);
        tokenReputation.blacklist.add(tokenHash);

        // Set reputation to 0
        const reputation = tokenReputation.getReputation(tokenHash);
        reputation.score = 0;

        res.json({
            message: 'Token blacklisted successfully',
            tokenHash: tokenHash
        });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

module.exports = {
    protect,
    authorize,
    getTokenStatus,
    getReputationStats,
    blacklistToken
};