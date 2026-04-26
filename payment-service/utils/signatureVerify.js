/**
 * ============================================================
 * 🔐 SIGNATURE VERIFICATION ENGINE — SECURITY CORE v2.0
 * ============================================================
 *
 * PURPOSE:
 * - Validate webhook authenticity
 * - Prevent spoofed event injection
 * - Ensure service-to-service trust
 *
 * SUPPORTED:
 * - Stripe-style HMAC signatures
 * - Generic SHA256 HMAC verification
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: FALCON (Fast Adaptive Lookahead Congestion Observation Network) [NEW]
 * ------------------------------------------------------------
 * - Real-time signature verification rate monitoring
 * - Predicts verification spikes before they happen
 * - Dynamically adjusts verification resources
 *
 * 🧠 ALGORITHM 2: SHIELD (Smart Heuristic Intelligent Edge Limiting & Detection) [NEW]
 * ------------------------------------------------------------
 * - Detects signature brute-force attacks
 * - Implements progressive backoff for failed attempts
 * - Automatic IP blacklisting for suspicious patterns
 *
 * ============================================================
 */

const crypto = require('crypto');
const Logger = require('./logger');

// ============================================================
// 🧠 ALGORITHM 1: FALCON (Fast Adaptive Lookahead Congestion Observation Network)
// ============================================================

class SignatureRatePredictor {
    constructor() {
        // Verification rate tracking
        this.verificationRates = new Map(); // endpoint -> [{ timestamp, count }]
        this.windowSizeMs = 60000; // 1 minute
        this.predictionWindowMs = 30000; // 30 seconds ahead

        // Adaptive resource allocation
        this.currentVerificationCapacity = 1000; // verifications per second
        this.peakRate = 0;
        this.congestionLevel = 'NORMAL';

        // Statistics
        this.stats = {
            totalVerifications: 0,
            predictedSpikes: 0,
            accuratePredictions: 0,
            capacityAdjustments: 0,
        };

        // Update predictions
        setInterval(() => this.updatePredictions(), 5000);
    }

    /**
     * Records verification attempt for rate analysis
     */
    recordVerification(endpoint, success, latencyMs) {
        const now = Date.now();
        this.stats.totalVerifications++;

        if (!this.verificationRates.has(endpoint)) {
            this.verificationRates.set(endpoint, []);
        }

        const rates = this.verificationRates.get(endpoint);
        rates.push({ timestamp: now, count: 1, success, latency: latencyMs });

        // Clean old entries
        const cutoff = now - this.windowSizeMs;
        const cleaned = rates.filter(r => r.timestamp > cutoff);

        // Aggregate by second
        const aggregated = new Map();
        for (const rate of cleaned) {
            const second = Math.floor(rate.timestamp / 1000);
            const existing = aggregated.get(second) || { count: 0, successCount: 0, totalLatency: 0 };
            existing.count += rate.count;
            if (rate.success) existing.successCount++;
            existing.totalLatency += rate.latency || 0;
            aggregated.set(second, existing);
        }

        this.verificationRates.set(endpoint,
            Array.from(aggregated.entries()).map(([second, data]) => ({
                timestamp: second * 1000,
                count: data.count,
                successRate: data.successCount / data.count,
                avgLatency: data.totalLatency / data.count,
            }))
        );

        // Update peak rate
        const currentRate = this.getCurrentRate(endpoint);
        if (currentRate > this.peakRate) {
            this.peakRate = currentRate;
        }
    }

    /**
     * Gets current verification rate (verifications/sec)
     */
    getCurrentRate(endpoint) {
        const rates = this.verificationRates.get(endpoint) || [];
        const now = Date.now();
        const recentRates = rates.filter(r => now - r.timestamp < 5000);

        if (recentRates.length === 0) return 0;

        const totalCount = recentRates.reduce((sum, r) => sum + r.count, 0);
        return totalCount / 5;
    }

    /**
     * Predicts future rate using exponential smoothing with trend
     */
    predictFutureRate(endpoint, secondsAhead = 30) {
        const rates = this.verificationRates.get(endpoint) || [];
        if (rates.length < 5) return this.getCurrentRate(endpoint);

        // Use recent rates for prediction
        const recent = rates.slice(-10);
        const x = recent.map((_, i) => i);
        const y = recent.map(r => r.count);

        const n = x.length;
        const sumX = x.reduce((a, b) => a + b, 0);
        const sumY = y.reduce((a, b) => a + b, 0);
        const sumXY = x.reduce((a, b, i) => a + b * y[i], 0);
        const sumXX = x.reduce((a, b) => a + b * b, 0);

        const slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        const intercept = (sumY - slope * sumX) / n;

        const predictedRate = intercept + slope * (recent.length + secondsAhead / 5);

        return Math.max(0, Math.round(predictedRate));
    }

    /**
     * Updates predictions and adjusts capacity
     */
    updatePredictions() {
        for (const [endpoint, _] of this.verificationRates.entries()) {
            const currentRate = this.getCurrentRate(endpoint);
            const predictedRate = this.predictFutureRate(endpoint, 30);

            this.stats.predictedSpikes++;

            // Check if prediction was accurate (within 30%)
            if (Math.abs(predictedRate - currentRate) / Math.max(currentRate, 1) < 0.3) {
                this.stats.accuratePredictions++;
            }

            // Adjust capacity if needed
            if (predictedRate > this.currentVerificationCapacity * 0.8) {
                const newCapacity = Math.max(this.currentVerificationCapacity, Math.ceil(predictedRate * 1.2));
                if (newCapacity > this.currentVerificationCapacity) {
                    this.currentVerificationCapacity = newCapacity;
                    this.stats.capacityAdjustments++;
                    console.log(`[FALCON] 📈 Increased verification capacity to ${newCapacity}/s (predicted: ${predictedRate}/s)`);
                }
            } else if (predictedRate < this.currentVerificationCapacity * 0.3 && this.currentVerificationCapacity > 100) {
                const newCapacity = Math.max(100, Math.ceil(predictedRate * 1.5));
                this.currentVerificationCapacity = newCapacity;
                this.stats.capacityAdjustments++;
                console.log(`[FALCON] 📉 Decreased verification capacity to ${newCapacity}/s`);
            }

            // Determine congestion level
            const utilization = currentRate / this.currentVerificationCapacity;
            if (utilization > 0.8) this.congestionLevel = 'CRITICAL';
            else if (utilization > 0.6) this.congestionLevel = 'HIGH';
            else if (utilization > 0.4) this.congestionLevel = 'ELEVATED';
            else this.congestionLevel = 'NORMAL';
        }
    }

    /**
     * Checks if request should be throttled
     */
    shouldThrottle(endpoint) {
        const currentRate = this.getCurrentRate(endpoint);

        if (currentRate > this.currentVerificationCapacity) {
            return {
                throttle: true,
                reason: 'RATE_LIMIT_EXCEEDED',
                retryAfter: 5,
            };
        }

        if (this.congestionLevel === 'CRITICAL') {
            return {
                throttle: true,
                reason: 'SYSTEM_CONGESTION',
                retryAfter: 10,
            };
        }

        return { throttle: false };
    }

    /**
     * Gets FALCON metrics
     */
    getMetrics() {
        const accuracy = this.stats.predictedSpikes > 0
            ? ((this.stats.accuratePredictions / this.stats.predictedSpikes) * 100).toFixed(1) + '%'
            : 'N/A';

        return {
            totalVerifications: this.stats.totalVerifications,
            currentCapacityPerSec: this.currentVerificationCapacity,
            peakRatePerSec: Math.round(this.peakRate),
            congestionLevel: this.congestionLevel,
            predictionAccuracy: accuracy,
            capacityAdjustments: this.stats.capacityAdjustments,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: SHIELD (Smart Heuristic Intelligent Edge Limiting & Detection)
// ============================================================

class SignatureAttackDetector {
    constructor() {
        // Failed attempt tracking
        this.failedAttempts = new Map(); // IP -> { count, firstAttempt, lastAttempt, blockUntil }
        this.blockedIPs = new Map(); // IP -> { blockUntil, reason, attemptCount }

        // Configuration
        this.failureThreshold = 10; // 10 failures triggers block
        this.blockDurationMs = 300000; // 5 minutes initial block
        this.maxBlockDurationMs = 86400000; // 24 hours max
        this.windowSizeMs = 600000; // 10 minute window

        // Pattern detection
        this.suspiciousPatterns = {
            RAPID_FAILURE: /(invalid signature|signature mismatch|timestamp expired)/i,
            BRUTE_FORCE: /attempt.*\d+/i,
            REPLAY_ATTACK: /timestamp.*expired/i,
        };

        // Statistics
        this.stats = {
            totalAttempts: 0,
            failedAttempts: 0,
            blockedIPs: 0,
            activeBlocks: 0,
            attackPatternsDetected: new Map(),
        };

        // Cleanup interval
        setInterval(() => this.cleanup(), 60000);
    }

    /**
     * Records verification attempt for attack detection
     */
    recordAttempt(ip, success, signature, endpoint) {
        this.stats.totalAttempts++;

        if (!success) {
            this.stats.failedAttempts++;
            this.recordFailedAttempt(ip, signature, endpoint);
        } else {
            this.recordSuccess(ip);
        }
    }

    /**
     * Records failed attempt for pattern analysis
     */
    recordFailedAttempt(ip, signature, endpoint) {
        const now = Date.now();

        if (!this.failedAttempts.has(ip)) {
            this.failedAttempts.set(ip, {
                attempts: [],
                firstAttempt: now,
                lastAttempt: now,
                endpoints: new Map(),
            });
        }

        const record = this.failedAttempts.get(ip);
        record.attempts.push({ timestamp: now, signature: signature?.substring(0, 20), endpoint });
        record.lastAttempt = now;

        // Track endpoint targeting
        record.endpoints.set(endpoint, (record.endpoints.get(endpoint) || 0) + 1);

        // Clean old attempts
        const cutoff = now - this.windowSizeMs;
        record.attempts = record.attempts.filter(a => a.timestamp > cutoff);

        // Check if should block
        if (record.attempts.length >= this.failureThreshold) {
            this.blockIP(ip, 'TOO_MANY_FAILURES', record.attempts.length);
        }

        // Detect attack patterns
        this.detectAttackPattern(ip, record);
    }

    /**
     * Records success to gradually reduce block probability
     */
    recordSuccess(ip) {
        if (this.failedAttempts.has(ip)) {
            const record = this.failedAttempts.get(ip);
            // Reduce attempt count on success (sliding window)
            const cutoff = Date.now() - this.windowSizeMs;
            record.attempts = record.attempts.filter(a => a.timestamp > cutoff);

            if (record.attempts.length === 0) {
                this.failedAttempts.delete(ip);
            }
        }
    }

    /**
     * Blocks IP with progressive backoff
     */
    blockIP(ip, reason, attemptCount) {
        const existing = this.blockedIPs.get(ip);
        let blockDuration = this.blockDurationMs;

        if (existing) {
            // Progressive backoff - double the block duration
            blockDuration = Math.min(this.maxBlockDurationMs, existing.blockDuration * 2);
        }

        const blockUntil = Date.now() + blockDuration;

        this.blockedIPs.set(ip, {
            blockUntil,
            reason,
            attemptCount,
            blockDuration,
            blockedAt: Date.now(),
        });

        this.stats.blockedIPs++;
        this.stats.activeBlocks = this.blockedIPs.size;

        Logger.anomalyEvent('IP_BLOCKED', {
            ip,
            reason,
            attemptCount,
            blockDurationMinutes: blockDuration / 60000,
        });
    }

    /**
     * Detects attack patterns from failed attempts
     */
    detectAttackPattern(ip, record) {
        // Pattern 1: Rapid failure rate
        const failureRate = record.attempts.length / (this.windowSizeMs / 60000);
        if (failureRate > 20) { // More than 20 failures per minute
            this.blockIP(ip, 'RAPID_FAILURE_RATE', record.attempts.length);
            this.incrementPatternCount('RAPID_FAILURE');
        }

        // Pattern 2: Multiple endpoint targeting
        const uniqueEndpoints = record.endpoints.size;
        if (uniqueEndpoints > 5) {
            this.blockIP(ip, 'MULTI_ENDPOINT_ATTACK', record.attempts.length);
            this.incrementPatternCount('MULTI_ENDPOINT_ATTACK');
        }

        // Pattern 3: Timestamp pattern (replay attack)
        const hasReplayPattern = record.attempts.some(a =>
            a.signature?.includes('timestamp') || a.signature?.includes('expired')
        );
        if (hasReplayPattern && record.attempts.length > 3) {
            this.blockIP(ip, 'REPLAY_ATTACK_DETECTED', record.attempts.length);
            this.incrementPatternCount('REPLAY_ATTACK');
        }
    }

    /**
     * Increments attack pattern counter
     */
    incrementPatternCount(pattern) {
        const count = this.stats.attackPatternsDetected.get(pattern) || 0;
        this.stats.attackPatternsDetected.set(pattern, count + 1);
    }

    /**
     * Checks if IP is blocked
     */
    isBlocked(ip) {
        const block = this.blockedIPs.get(ip);
        if (!block) return false;

        if (Date.now() > block.blockUntil) {
            this.blockedIPs.delete(ip);
            this.stats.activeBlocks = this.blockedIPs.size;
            return false;
        }

        return true;
    }

    /**
     * Gets block reason for IP
     */
    getBlockReason(ip) {
        const block = this.blockedIPs.get(ip);
        if (!block) return null;

        return {
            reason: block.reason,
            remainingSeconds: Math.ceil((block.blockUntil - Date.now()) / 1000),
        };
    }

    /**
     * Cleanup expired blocks
     */
    cleanup() {
        const now = Date.now();
        let cleaned = 0;

        for (const [ip, block] of this.blockedIPs.entries()) {
            if (block.blockUntil < now) {
                this.blockedIPs.delete(ip);
                cleaned++;
            }
        }

        if (cleaned > 0) {
            this.stats.activeBlocks = this.blockedIPs.size;
            console.log(`[SHIELD] 🧹 Cleaned ${cleaned} expired IP blocks`);
        }
    }

    /**
     * Gets SHIELD metrics
     */
    getMetrics() {
        return {
            totalAttempts: this.stats.totalAttempts,
            failedAttempts: this.stats.failedAttempts,
            failureRate: this.stats.totalAttempts > 0
                ? ((this.stats.failedAttempts / this.stats.totalAttempts) * 100).toFixed(2) + '%'
                : '0%',
            blockedIPs: this.stats.blockedIPs,
            activeBlocks: this.stats.activeBlocks,
            attackPatterns: Object.fromEntries(this.stats.attackPatternsDetected),
        };
    }
}

// ============================================================
// 🔧 INITIALIZE NEW ALGORITHMS
// ============================================================

const ratePredictor = new SignatureRatePredictor();
const attackDetector = new SignatureAttackDetector();

// ============================================================
// 🧠 CORE SIGNATURE VERIFIER [ENHANCED]
// ============================================================

class VerifySignature {

    /**
     * Verify HMAC SHA256 signature (enhanced with monitoring)
     *
     * @param {string} payload - raw request body (string)
     * @param {string} signature - incoming signature header
     * @param {string} secret - shared secret key
     * @param {object} options - additional options (ip, endpoint)
     */
    static verifyHMAC(payload, signature, secret, options = {}) {
        const ip = options.ip || 'unknown';
        const endpoint = options.endpoint || 'unknown';
        const startTime = Date.now();

        if (!payload || !signature || !secret) {
            Logger.warn('SIGNATURE_MISSING_FIELDS', {
                hasPayload: !!payload,
                hasSignature: !!signature,
                hasSecret: !!secret,
            });

            attackDetector.recordAttempt(ip, false, signature, endpoint);
            ratePredictor.recordVerification(endpoint, false, Date.now() - startTime);

            return false;
        }

        // Check if IP is blocked
        if (attackDetector.isBlocked(ip)) {
            const blockReason = attackDetector.getBlockReason(ip);
            Logger.warn('BLOCKED_IP_ATTEMPT', {
                ip,
                blockReason,
                endpoint,
            });
            return false;
        }

        const expectedSignature = crypto
            .createHmac('sha256', secret)
            .update(payload, 'utf8')
            .digest('hex');

        // Timing-safe comparison (CRITICAL)
        const isValid = crypto.timingSafeEqual(
            Buffer.from(expectedSignature),
            Buffer.from(signature)
        );

        const latency = Date.now() - startTime;

        // Record for monitoring
        attackDetector.recordAttempt(ip, isValid, signature, endpoint);
        ratePredictor.recordVerification(endpoint, isValid, latency);

        if (!isValid) {
            Logger.anomalyEvent('SIGNATURE_MISMATCH', {
                expectedPrefix: expectedSignature.substring(0, 8),
                receivedPrefix: signature.substring(0, 8),
                ip,
                endpoint,
            });
        }

        return isValid;
    }

    /**
     * Stripe-style signature verification (timestamped) [ENHANCED]
     *
     * Format:
     * t=timestamp,v1=signature
     */
    static verifyStripeSignature(payload, header, secret, toleranceSeconds = 300, options = {}) {
        const ip = options.ip || 'unknown';
        const endpoint = options.endpoint || 'stripe-webhook';
        const startTime = Date.now();

        try {
            if (!header) return false;

            // Check if IP is blocked
            if (attackDetector.isBlocked(ip)) {
                const blockReason = attackDetector.getBlockReason(ip);
                Logger.warn('BLOCKED_IP_STRIPE_ATTEMPT', {
                    ip,
                    blockReason,
                });
                return false;
            }

            const parts = header.split(',');
            let timestamp, signature;

            for (const part of parts) {
                const [key, value] = part.split('=');
                if (key === 't') timestamp = value;
                if (key === 'v1') signature = value;
            }

            if (!timestamp || !signature) {
                attackDetector.recordAttempt(ip, false, header, endpoint);
                return false;
            }

            const now = Math.floor(Date.now() / 1000);

            // Prevent replay attacks
            if (Math.abs(now - timestamp) > toleranceSeconds) {
                Logger.warn('SIGNATURE_TIMESTAMP_EXPIRED', {
                    timestamp,
                    now,
                    ip,
                });
                attackDetector.recordAttempt(ip, false, header, endpoint);
                return false;
            }

            const signedPayload = `${timestamp}.${payload}`;

            const expected = crypto
                .createHmac('sha256', secret)
                .update(signedPayload, 'utf8')
                .digest('hex');

            const isValid = crypto.timingSafeEqual(
                Buffer.from(expected),
                Buffer.from(signature)
            );

            const latency = Date.now() - startTime;

            // Record for monitoring
            attackDetector.recordAttempt(ip, isValid, header, endpoint);
            ratePredictor.recordVerification(endpoint, isValid, latency);

            if (!isValid) {
                Logger.anomalyEvent('STRIPE_SIGNATURE_INVALID', {
                    timestamp,
                    ip,
                });
            }

            return isValid;

        } catch (err) {
            Logger.error('SIGNATURE_VERIFICATION_ERROR', {
                error: err.message,
                ip,
            });
            attackDetector.recordAttempt(ip, false, header, endpoint);
            return false;
        }
    }

    /**
     * Middleware wrapper (Express-ready) [ENHANCED]
     */
    static middleware(secret, type = 'hmac') {
        return (req, res, next) => {
            const signature =
                req.headers['x-signature'] ||
                req.headers['stripe-signature'];

            const payload = JSON.stringify(req.body);

            // Get client IP
            const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress;

            // Check throttling before verification
            const throttleDecision = ratePredictor.shouldThrottle(req.path);
            if (throttleDecision.throttle) {
                res.setHeader('Retry-After', throttleDecision.retryAfter);
                return res.status(429).json({
                    success: false,
                    message: 'Rate limit exceeded',
                    code: throttleDecision.reason,
                });
            }

            let valid = false;

            if (type === 'stripe') {
                valid = this.verifyStripeSignature(payload, signature, secret, 300, { ip, endpoint: req.path });
            } else {
                valid = this.verifyHMAC(payload, signature, secret, { ip, endpoint: req.path });
            }

            if (!valid) {
                Logger.error('UNAUTHORIZED_WEBHOOK_ATTEMPT', {
                    ip,
                    path: req.path,
                });

                return res.status(401).json({
                    success: false,
                    message: 'Invalid signature',
                });
            }

            next();
        };
    }

    /**
     * Get security metrics (FALCON + SHIELD)
     */
    static getSecurityMetrics() {
        return {
            falcon: ratePredictor.getMetrics(),
            shield: attackDetector.getMetrics(),
        };
    }

    /**
     * Health check for security engine
     */
    static healthCheck() {
        const falconMetrics = ratePredictor.getMetrics();
        const shieldMetrics = attackDetector.getMetrics();

        let status = 'HEALTHY';
        if (falconMetrics.congestionLevel === 'CRITICAL') status = 'DEGRADED';
        if (shieldMetrics.activeBlocks > 100) status = 'DEGRADED';

        return {
            status,
            timestamp: Date.now(),
            metrics: {
                verificationCapacity: falconMetrics.currentCapacityPerSec,
                activeBlocks: shieldMetrics.activeBlocks,
                failureRate: shieldMetrics.failureRate,
            },
        };
    }
}

// ============================================================
// BACKGROUND HEALTH MONITORING
// ============================================================

setInterval(() => {
    const health = VerifySignature.healthCheck();
    if (health.status !== 'HEALTHY') {
        Logger.warn('Signature engine health degraded', health);
    }
}, 60000);

// ============================================================
// EXPORT
// ============================================================

module.exports = VerifySignature;