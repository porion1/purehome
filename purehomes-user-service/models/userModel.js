const mongoose = require('mongoose');
const crypto = require('crypto');

/**
 * 🚀 SIF (Selective Insight Framework)
 * Production-safe adaptive index intelligence
 */
class SelectiveInsightEngine {
    constructor(collectionName) {
        this.collectionName = collectionName;

        // Smart storage (bounded)
        this.insights = new Map();

        // Controls
        this.enabled = process.env.SIF === 'true';
        this.maxEntries = 1000;
    }

    record(queryFields, duration) {
        if (!this.enabled) return;

        // 🚫 Ignore fast queries
        if (duration < 50) return;

        const key = queryFields.sort().join(',');

        const rarity = 1 / (this.insights.get(key)?.count + 1 || 1);
        const fieldWeight = queryFields.length || 1;

        // 🧠 SIF Score
        const score = duration * rarity * fieldWeight;

        if (this.insights.has(key)) {
            const existing = this.insights.get(key);
            existing.count++;
            existing.score = (existing.score + score) / 2;
            existing.lastSeen = Date.now();
        } else {
            if (this.insights.size >= this.maxEntries) {
                this._evict();
            }

            this.insights.set(key, {
                fields: queryFields,
                count: 1,
                score,
                lastSeen: Date.now()
            });
        }
    }

    _evict() {
        // Remove lowest score entry
        const lowest = [...this.insights.entries()]
            .sort((a, b) => a[1].score - b[1].score)[0];

        if (lowest) this.insights.delete(lowest[0]);
    }

    getSuggestions() {
        return [...this.insights.values()]
            .filter(i => i.score > 100)
            .map(i => ({
                type: i.fields.length > 1 ? 'composite' : 'single',
                fields: i.fields,
                score: i.score
            }));
    }
}

/**
 * 🔮 TAP (Token Anomaly Prediction)
 * Predicts token misuse before it happens using behavioral fingerprinting
 *
 * How it works:
 * 1. Builds behavioral baseline for each user (login times, request patterns)
 * 2. Calculates anomaly score on each request (0-100)
 * 3. Triggers progressive security responses based on score
 * 4. Self-learning - adapts to user behavior over time
 */
class TokenAnomalyPredictor {
    constructor() {
        this.userBehavior = new Map(); // userId -> behavior profile
        this.globalBaseline = {
            avgRequestsPerHour: 0,
            avgLoginInterval: 0,
            commonUserAgents: new Map()
        };
        this.anomalyThresholds = {
            low: 30,      // Log unusual activity
            medium: 60,   // Require additional verification
            high: 85      // Force re-authentication
        };

        // Learning mode - builds baseline over time
        this.learningMode = true;
        this.samplesNeeded = 50;

        // Cleanup old profiles every hour
        setInterval(() => this._cleanupOldProfiles(), 3600000);
    }

    /**
     * Record successful login for user
     */
    recordLogin(userId, req) {
        const now = Date.now();
        const fingerprint = this._generateFingerprint(req);

        if (!this.userBehavior.has(userId)) {
            this.userBehavior.set(userId, {
                logins: [],
                requests: [],
                avgRequestInterval: 0,
                lastLoginAt: now,
                lastRequestAt: now,
                establishedPattern: false,
                anomalyHistory: []
            });
        }

        const profile = this.userBehavior.get(userId);
        profile.logins.push({
            timestamp: now,
            fingerprint,
            ip: req.ip,
            userAgent: req.headers['user-agent']
        });

        // Keep last 20 logins
        if (profile.logins.length > 20) profile.logins.shift();

        profile.lastLoginAt = now;

        // Update global baseline
        this._updateGlobalBaseline(profile);

        // Check if pattern is established
        if (profile.logins.length >= this.samplesNeeded) {
            profile.establishedPattern = true;
            this.learningMode = false;
        }
    }

    /**
     * Predict anomaly score for current request
     * Returns score 0-100 (higher = more anomalous)
     */
    predict(userId, req) {
        if (!this.userBehavior.has(userId)) {
            return { score: 0, risk: 'unknown', action: 'allow' };
        }

        const profile = this.userBehavior.get(userId);

        // Not enough data yet - allow all
        if (!profile.establishedPattern) {
            return { score: 0, risk: 'learning', action: 'allow' };
        }

        let anomalyScore = 0;
        const reasons = [];

        // 1. Time-based anomaly (login at unusual hour)
        const currentHour = new Date().getHours();
        const usualHours = profile.logins.map(l => new Date(l.timestamp).getHours());
        const hourDeviation = this._calculateDeviation(currentHour, usualHours);
        if (hourDeviation > 5) {
            anomalyScore += 25;
            reasons.push('unusual_hour');
        }

        // 2. IP location anomaly
        const currentIp = req.ip;
        const usualIps = profile.logins.map(l => l.ip);
        if (!usualIps.includes(currentIp) && usualIps.length > 3) {
            anomalyScore += 30;
            reasons.push('unusual_ip');
        }

        // 3. Request frequency anomaly
        const now = Date.now();
        const requestsLastMinute = profile.requests.filter(r => now - r.timestamp < 60000).length;
        if (requestsLastMinute > 60) {
            anomalyScore += 20;
            reasons.push('high_frequency');
        }

        // 4. User agent anomaly
        const currentUA = req.headers['user-agent'];
        const usualUAs = profile.logins.map(l => l.userAgent);
        if (!usualUAs.includes(currentUA) && usualUAs.length > 2) {
            anomalyScore += 25;
            reasons.push('unusual_user_agent');
        }

        // 5. Time since last login anomaly
        const hoursSinceLastLogin = (now - profile.lastLoginAt) / 3600000;
        const avgLoginInterval = this._calculateAvgLoginInterval(profile);
        if (avgLoginInterval > 0 && hoursSinceLastLogin > avgLoginInterval * 3) {
            anomalyScore += 15;
            reasons.push('unusual_gap');
        }

        // Cap at 100
        anomalyScore = Math.min(100, anomalyScore);

        // Record this request
        profile.requests.push({ timestamp: now, score: anomalyScore });
        if (profile.requests.length > 100) profile.requests.shift();

        profile.anomalyHistory.push({ timestamp: now, score: anomalyScore });
        if (profile.anomalyHistory.length > 50) profile.anomalyHistory.shift();

        // Determine action based on score
        let action = 'allow';
        let risk = 'low';

        if (anomalyScore >= this.anomalyThresholds.high) {
            action = 'force_reauth';
            risk = 'critical';
        } else if (anomalyScore >= this.anomalyThresholds.medium) {
            action = 'require_2fa';
            risk = 'high';
        } else if (anomalyScore >= this.anomalyThresholds.low) {
            action = 'log_only';
            risk = 'medium';
        }

        return {
            score: Math.round(anomalyScore),
            risk,
            action,
            reasons,
            shouldBlock: action === 'force_reauth'
        };
    }

    /**
     * Generate request fingerprint
     */
    _generateFingerprint(req) {
        const components = [
            req.ip,
            req.headers['user-agent'],
            req.headers['accept-language'],
            req.headers['sec-ch-ua-platform']
        ];
        return crypto.createHash('sha256').update(components.join('|')).digest('hex');
    }

    /**
     * Calculate standard deviation-like metric
     */
    _calculateDeviation(value, samples) {
        if (samples.length === 0) return 0;
        const avg = samples.reduce((a, b) => a + b, 0) / samples.length;
        return Math.abs(value - avg);
    }

    /**
     * Calculate average login interval
     */
    _calculateAvgLoginInterval(profile) {
        if (profile.logins.length < 2) return 0;
        let totalInterval = 0;
        for (let i = 1; i < profile.logins.length; i++) {
            totalInterval += profile.logins[i].timestamp - profile.logins[i-1].timestamp;
        }
        return totalInterval / (profile.logins.length - 1) / 3600000; // in hours
    }

    /**
     * Update global baseline for better predictions
     */
    _updateGlobalBaseline(profile) {
        const totalLogins = profile.logins.length;
        if (totalLogins > 0) {
            // Update average login interval
            const intervals = [];
            for (let i = 1; i < totalLogins; i++) {
                intervals.push(profile.logins[i].timestamp - profile.logins[i-1].timestamp);
            }
            if (intervals.length > 0) {
                const avgInterval = intervals.reduce((a, b) => a + b, 0) / intervals.length;
                this.globalBaseline.avgLoginInterval =
                    (this.globalBaseline.avgLoginInterval + avgInterval) / 2;
            }
        }
    }

    /**
     * Cleanup old profiles (inactive users)
     */
    _cleanupOldProfiles() {
        const thirtyDaysAgo = Date.now() - 30 * 24 * 3600000;
        for (const [userId, profile] of this.userBehavior.entries()) {
            if (profile.lastLoginAt < thirtyDaysAgo) {
                this.userBehavior.delete(userId);
            }
        }
    }

    /**
     * Get security metrics
     */
    getMetrics() {
        return {
            activeProfiles: this.userBehavior.size,
            learningMode: this.learningMode,
            samplesNeeded: this.samplesNeeded,
            globalBaseline: {
                avgLoginInterval: Math.round(this.globalBaseline.avgLoginInterval / 3600000),
                commonUserAgents: this.globalBaseline.commonUserAgents.size
            },
            anomalyThresholds: this.anomalyThresholds
        };
    }
}

// ================== SCHEMA ==================

const userSchema = new mongoose.Schema(
    {
        name: { type: String, required: true, trim: true },
        email: { type: String, required: true, unique: true, lowercase: true },
        password: { type: String, select: false },

        isGuest: { type: Boolean, default: false },
        emailVerified: { type: Boolean, default: false },

        lastLoginAt: Date,
        loginCount: { type: Number, default: 0 },

        role: { type: String, enum: ['user', 'admin'], default: 'user' },

        preferences: {
            notifications: { type: Boolean, default: true },
            language: { type: String, default: 'en' }
        },

        refreshToken: { type: String, select: false },
        passwordResetToken: String,
        passwordResetExpires: Date,
        deletedAt: Date,

        // TAP-specific fields
        securityFlags: {
            requiresReauth: { type: Boolean, default: false },
            anomalyCount: { type: Number, default: 0 },
            lastAnomalyAt: Date
        }
    },
    { timestamps: true }
);

const insightEngine = new SelectiveInsightEngine('users');
const anomalyPredictor = new TokenAnomalyPredictor();

// ================== SMART TRACKING (SAFE) ==================

userSchema.pre('findOne', function () {
    this._startTime = Date.now();
});

userSchema.post('findOne', function () {
    if (!this._startTime) return;

    const duration = Date.now() - this._startTime;

    // 🔥 Sample only 1% (massive protection)
    if (Math.random() > 0.01) return;

    const fields = Object.keys(this.getQuery());

    // 🧠 Non-blocking + throttled
    setTimeout(() => {
        try {
            insightEngine.record(fields, duration);
        } catch (err) {
            // Silent fail - production safe
        }
    }, 5);
});

// ================== METHODS ==================

userSchema.methods.comparePassword = async function (candidate) {
    if (!this.password) return false;
    const bcrypt = require('bcrypt');
    return bcrypt.compare(candidate, this.password);
};

userSchema.methods.generatePasswordResetToken = function () {
    const token = crypto.randomBytes(32).toString('hex');
    this.passwordResetToken = crypto
        .createHash('sha256')
        .update(token)
        .digest('hex');
    this.passwordResetExpires = Date.now() + 3600000;
    return token;
};

userSchema.methods.recordLogin = function () {
    this.lastLoginAt = new Date();
    this.loginCount++;
    return this.save({ validateBeforeSave: false });
};

/**
 * TAP: Check if request is anomalous
 */
userSchema.methods.checkAnomaly = function (req) {
    return anomalyPredictor.predict(this._id.toString(), req);
};

/**
 * TAP: Record successful login for anomaly baseline
 */
userSchema.methods.recordLoginForAnomaly = function (req) {
    anomalyPredictor.recordLogin(this._id.toString(), req);
};

/**
 * TAP: Update security flags based on anomaly
 */
userSchema.methods.updateSecurityFlags = async function (anomalyResult) {
    if (anomalyResult.shouldBlock) {
        this.securityFlags.requiresReauth = true;
        this.securityFlags.anomalyCount++;
        this.securityFlags.lastAnomalyAt = new Date();
        await this.save({ validateBeforeSave: false });
    }
    return this;
};

// ================== STATICS ==================

userSchema.statics.findOrCreateGuest = async function (sessionId) {
    const email = `guest_${sessionId}@example.com`;

    return this.findOneAndUpdate(
        { email, isGuest: true },
        {
            $setOnInsert: {
                name: `Guest_${sessionId.slice(-6)}`,
                email,
                isGuest: true
            }
        },
        { new: true, upsert: true }
    );
};

userSchema.statics.getActiveUsersCount = function () {
    return this.countDocuments({
        lastLoginAt: { $gte: new Date(Date.now() - 30 * 86400000) },
        isGuest: false
    });
};

/**
 * TAP: Get anomaly metrics for monitoring
 */
userSchema.statics.getAnomalyMetrics = function () {
    return anomalyPredictor.getMetrics();
};

// ================== INDEXES ==================

userSchema.index({ email: 1, isGuest: 1 });
userSchema.index({ lastLoginAt: -1 });
userSchema.index({ 'securityFlags.requiresReauth': 1 });
userSchema.index(
    { createdAt: 1 },
    {
        expireAfterSeconds: 2592000,
        partialFilterExpression: { isGuest: true }
    }
);

// ================== MODEL ==================

const User =
    mongoose.models.users || mongoose.model('users', userSchema);

User.getIndexSuggestions = () => insightEngine.getSuggestions();
User.getAnomalyMetrics = User.getAnomalyMetrics;

module.exports = User;