// ----------------------------
// Algorithm 1: Adaptive Input Validation Engine (AIVE) - Existing
// ----------------------------
// This engine dynamically:
// 1. Validates fields using schema rules
// 2. Normalizes inputs (trim, lowercase, etc.)
// 3. Assigns validationScore per request
//
// validationScore = validFields / totalFields
//
// If score < threshold → reject request
//
// Benefits:
// - Flexible validation rules
// - Partial tolerance (future-ready)
// - Centralized validation logic
// ----------------------------

// Utility: normalize input
const normalizeValue = (value, rules) => {
    if (typeof value === 'string') {
        if (rules.trim) value = value.trim();
        if (rules.lowercase) value = value.toLowerCase();
    }
    return value;
};

// Utility: validate single field
const validateField = (key, value, rules) => {
    let valid = true;
    let message = '';

    if (rules.required && (value === undefined || value === null || value === '')) {
        return { valid: false, message: `${key} is required` };
    }

    if (value !== undefined) {
        if (rules.type && typeof value !== rules.type) {
            return { valid: false, message: `${key} must be of type ${rules.type}` };
        }

        if (rules.min !== undefined && value < rules.min) {
            return { valid: false, message: `${key} must be >= ${rules.min}` };
        }

        if (rules.max !== undefined && value > rules.max) {
            return { valid: false, message: `${key} must be <= ${rules.max}` };
        }

        if (rules.enum && !rules.enum.includes(value)) {
            return { valid: false, message: `${key} must be one of ${rules.enum.join(', ')}` };
        }

        // Pattern matching (regex)
        if (rules.pattern && !rules.pattern.test(value)) {
            return { valid: false, message: `${key} format is invalid` };
        }
    }

    return { valid, message };
};

// ----------------------------
// Algorithm 2: Predictive Anomaly Detection & Injection Prevention Engine (PADIPE) - NEW
// ----------------------------
// FAANG-level security engine that:
// 1. Detects SQL injection, NoSQL injection, and XSS patterns
// 2. Uses behavioral scoring to identify attack patterns
// 3. Implements sliding window anomaly detection
// 4. Auto-blocks IPs with high anomaly scores
// 5. Provides predictive threat intelligence
// ----------------------------

class AnomalyDetectionEngine {
    constructor() {
        this.ipBlacklist = new Map(); // IP -> block expiry timestamp
        this.anomalyStore = new Map(); // IP -> anomaly records
        this.patternLibrary = this.loadPatterns();
        this.requestHistory = new Map(); // IP -> request pattern history
        this.cleanupInterval = null;

        // Configuration
        this.anomalyThreshold = 10; // Score > 10 = suspicious
        this.blockThreshold = 25; // Score > 25 = auto-block
        this.blockDuration = 3600000; // 1 hour block
        this.windowSize = 60000; // 1 minute sliding window

        // Start background tasks
        this.startCleanup();
    }

    // Load security patterns (SQLi, NoSQLi, XSS)
    loadPatterns() {
        return {
            // SQL Injection patterns
            sqlInjection: [
                /(\%27)|(\')|(\-\-)|(\%23)|(#)/i,
                /(\%24)/i,
                /select.+from/i,
                /insert.+into/i,
                /delete.+from/i,
                /drop.+table/i,
                /union.+select/i,
                /or\s+1\s*=\s*1/i,
                /or\s+1\s*=\s*2/i,
                /and\s+1\s*=\s*1/i,
                /exec(\s|\+)+(s|x)p\w+/i
            ],

            // NoSQL Injection patterns
            nosqlInjection: [
                /\$gt/,
                /\$lt/,
                /\$gte/,
                /\$lte/,
                /\$ne/,
                /\$in/,
                /\$nin/,
                /\$or/,
                /\$and/,
                /\$where/,
                /\$regex/,
                /\$exists/,
                /\$type/
            ],

            // XSS patterns
            xss: [
                /<script[^>]*>.*?<\/script>/i,
                /javascript:/i,
                /onerror\s*=/i,
                /onload\s*=/i,
                /onclick\s*=/i,
                /eval\s*\(/i,
                /document\.cookie/i,
                /alert\s*\(/i,
                /<iframe[^>]*>/i,
                /<img[^>]*onerror/i
            ],

            // Path traversal
            pathTraversal: [
                /\.\.\//,
                /\.\.\\/,
                /\/etc\/passwd/,
                /\/var\/log/,
                /c:\\windows\\system32/i
            ],

            // Command injection
            commandInjection: [
                /;.*sh\s/,
                /\|\s*sh\s/,
                /`.*`/,
                /\$\{.*\}/,
                /&&\s*\w+/,
                /\|\|\s*\w+/
            ]
        };
    }

    // Scan input for attack patterns
    scanForPatterns(input, fieldName) {
        if (!input || typeof input !== 'string') return { detected: false, patterns: [], score: 0 };

        const detected = [];
        let totalScore = 0;

        // Check each pattern category
        for (const [category, patterns] of Object.entries(this.patternLibrary)) {
            for (const pattern of patterns) {
                if (pattern.test(input)) {
                    detected.push({
                        category,
                        pattern: pattern.toString(),
                        field: fieldName,
                        sample: input.substring(0, 50) // Truncate for logging
                    });
                    totalScore += this.getCategoryWeight(category);
                }
            }
        }

        return {
            detected: detected.length > 0,
            patterns: detected,
            score: Math.min(100, totalScore)
        };
    }

    // Get weight for each attack category
    getCategoryWeight(category) {
        const weights = {
            sqlInjection: 15,
            nosqlInjection: 20,
            xss: 12,
            pathTraversal: 10,
            commandInjection: 25
        };
        return weights[category] || 5;
    }

    // Detect behavioral anomalies (request frequency, payload size)
    detectBehavioralAnomaly(ip, requestData) {
        if (!this.requestHistory.has(ip)) {
            this.requestHistory.set(ip, []);
        }

        const history = this.requestHistory.get(ip);
        const now = Date.now();

        // Add current request to history
        history.push({
            timestamp: now,
            payloadSize: JSON.stringify(requestData).length,
            fieldCount: Object.keys(requestData).length
        });

        // Remove old entries outside window
        while (history.length > 0 && history[0].timestamp < now - this.windowSize) {
            history.shift();
        }

        let anomalyScore = 0;

        // Detect rapid-fire requests (> 30 requests per minute)
        if (history.length > 30) {
            anomalyScore += Math.floor(history.length / 10);
        }

        // Detect unusually large payloads (> 10KB)
        const avgPayloadSize = history.reduce((sum, h) => sum + h.payloadSize, 0) / history.length;
        if (avgPayloadSize > 10240) { // 10KB
            anomalyScore += 5;
        }

        // Detect excessive field counts (> 50 fields)
        const avgFieldCount = history.reduce((sum, h) => sum + h.fieldCount, 0) / history.length;
        if (avgFieldCount > 50) {
            anomalyScore += 8;
        }

        return anomalyScore;
    }

    // Check if IP is blocked
    isIpBlocked(ip) {
        if (this.ipBlacklist.has(ip)) {
            const expiry = this.ipBlacklist.get(ip);
            if (Date.now() < expiry) {
                return true;
            } else {
                this.ipBlacklist.delete(ip);
            }
        }
        return false;
    }

    // Block IP for specified duration
    blockIp(ip, reason, duration = this.blockDuration) {
        this.ipBlacklist.set(ip, Date.now() + duration);
        console.error(`🚫 IP blocked: ${ip} - ${reason} (${duration/1000}s)`);

        // Store block reason
        if (!this.anomalyStore.has(ip)) {
            this.anomalyStore.set(ip, []);
        }
        const records = this.anomalyStore.get(ip);
        records.push({
            timestamp: Date.now(),
            type: 'BLOCK',
            reason: reason,
            score: 100
        });
    }

    // Record anomaly for IP
    recordAnomaly(ip, anomalyType, score, details) {
        if (!this.anomalyStore.has(ip)) {
            this.anomalyStore.set(ip, []);
        }

        const records = this.anomalyStore.get(ip);
        records.push({
            timestamp: Date.now(),
            type: anomalyType,
            score: score,
            details: details
        });

        // Keep only last 100 records
        if (records.length > 100) {
            records.shift();
        }

        // Calculate total anomaly score for IP
        const recentAnomalies = records.filter(r => Date.now() - r.timestamp < 300000); // Last 5 minutes
        const totalScore = recentAnomalies.reduce((sum, r) => sum + (r.score || 5), 0);

        // Auto-block if threshold exceeded
        if (totalScore >= this.blockThreshold && !this.isIpBlocked(ip)) {
            this.blockIp(ip, `Exceeded anomaly threshold: ${totalScore}/${this.blockThreshold}`);
        }

        return totalScore;
    }

    // Get IP threat level
    getThreatLevel(ip) {
        if (this.isIpBlocked(ip)) return 'BLOCKED';

        const records = this.anomalyStore.get(ip) || [];
        const recentAnomalies = records.filter(r => Date.now() - r.timestamp < 300000);
        const totalScore = recentAnomalies.reduce((sum, r) => sum + (r.score || 5), 0);

        if (totalScore >= this.blockThreshold) return 'CRITICAL';
        if (totalScore >= this.anomalyThreshold) return 'SUSPICIOUS';
        if (totalScore >= 5) return 'WATCHING';
        return 'CLEAN';
    }

    // Get anomaly statistics
    getStats() {
        let totalAnomalies = 0;
        let blockedCount = this.ipBlacklist.size;

        for (const records of this.anomalyStore.values()) {
            totalAnomalies += records.length;
        }

        return {
            blockedIPs: blockedCount,
            trackedIPs: this.anomalyStore.size,
            totalAnomalies: totalAnomalies,
            patternsLoaded: Object.values(this.patternLibrary).flat().length
        };
    }

    // Background cleanup
    startCleanup() {
        setInterval(() => {
            const cutoff = Date.now() - 86400000; // 24 hours

            // Clean old anomaly records
            for (const [ip, records] of this.anomalyStore) {
                const filtered = records.filter(r => r.timestamp > cutoff);
                if (filtered.length === 0) {
                    this.anomalyStore.delete(ip);
                } else {
                    this.anomalyStore.set(ip, filtered);
                }
            }

            // Clean expired blacklist entries
            for (const [ip, expiry] of this.ipBlacklist) {
                if (Date.now() > expiry) {
                    this.ipBlacklist.delete(ip);
                }
            }
        }, 3600000); // Every hour
    }
}

// Initialize anomaly detection engine
const anomalyEngine = new AnomalyDetectionEngine();

// ----------------------------
// Enhanced Main Validator Middleware Factory (Algorithm 1 + Algorithm 2)
// ----------------------------
const validateInput = (schema, options = {}) => {
    const {
        strictMode = true,
        anomalyDetection = true,
        blockOnAnomaly = true
    } = options;

    return (req, res, next) => {
        const data = req.body;
        const clientIp = req.ip || req.connection.remoteAddress || req.headers['x-forwarded-for'] || 'unknown';

        // Algorithm 2: Check if IP is blocked
        if (anomalyDetection && anomalyEngine.isIpBlocked(clientIp)) {
            return res.status(403).json({
                message: 'Access denied',
                error: 'Your IP has been temporarily blocked due to suspicious activity',
                timestamp: new Date().toISOString()
            });
        }

        // Algorithm 2: Scan request for attack patterns
        let totalAnomalyScore = 0;
        const detectedAttacks = [];

        if (anomalyDetection) {
            // Scan all fields for injection patterns
            for (const [key, value] of Object.entries(data)) {
                if (typeof value === 'string') {
                    const result = anomalyEngine.scanForPatterns(value, key);
                    if (result.detected) {
                        detectedAttacks.push(...result.patterns);
                        totalAnomalyScore += result.score;
                    }
                } else if (typeof value === 'object' && value !== null) {
                    // Recursively scan nested objects
                    const scanNested = (obj, path) => {
                        for (const [k, v] of Object.entries(obj)) {
                            const fullPath = `${path}.${k}`;
                            if (typeof v === 'string') {
                                const result = anomalyEngine.scanForPatterns(v, fullPath);
                                if (result.detected) {
                                    detectedAttacks.push(...result.patterns);
                                    totalAnomalyScore += result.score;
                                }
                            } else if (typeof v === 'object' && v !== null) {
                                scanNested(v, fullPath);
                            }
                        }
                    };
                    scanNested(value, key);
                }
            }

            // Detect behavioral anomalies
            const behavioralScore = anomalyEngine.detectBehavioralAnomaly(clientIp, data);
            totalAnomalyScore += behavioralScore;

            // Record anomaly if detected
            if (detectedAttacks.length > 0 || behavioralScore > 0) {
                const anomalyScore = Math.min(100, totalAnomalyScore);
                anomalyEngine.recordAnomaly(clientIp, 'ATTACK_PATTERN', anomalyScore, {
                    attacks: detectedAttacks,
                    behavioralScore: behavioralScore,
                    timestamp: Date.now()
                });

                // Block if severe attack detected
                if (blockOnAnomaly && totalAnomalyScore >= anomalyEngine.blockThreshold) {
                    return res.status(403).json({
                        message: 'Request blocked due to security violation',
                        threatLevel: anomalyEngine.getThreatLevel(clientIp),
                        attackTypes: [...new Set(detectedAttacks.map(a => a.category))]
                    });
                }
            }
        }

        // Algorithm 1: Validate input fields
        let validFields = 0;
        let totalFields = Object.keys(schema).length;
        let errors = {};
        const sanitizedData = {};

        for (const key in schema) {
            const rules = schema[key];
            let value = data[key];

            // Normalize value (Algorithm 1)
            value = normalizeValue(value, rules);

            // Validate field (Algorithm 1)
            const result = validateField(key, value, rules);

            if (!result.valid) {
                errors[key] = result.message;
            } else {
                validFields++;
                sanitizedData[key] = value;
            }
        }

        // Calculate validation score (Algorithm 1)
        const validationScore = validFields / totalFields;

        // Algorithm 2: Adjust threshold based on threat level
        const threatLevel = anomalyEngine.getThreatLevel(clientIp);
        let requiredScore = strictMode ? 1.0 : 0.8;

        if (threatLevel === 'SUSPICIOUS') {
            requiredScore = 1.0; // Stricter for suspicious IPs
        } else if (threatLevel === 'WATCHING') {
            requiredScore = 0.95;
        }

        // Check validation score
        if (validationScore < requiredScore) {
            // Record validation failure as anomaly
            if (anomalyDetection) {
                anomalyEngine.recordAnomaly(clientIp, 'VALIDATION_FAILURE', 3, {
                    validationScore: validationScore,
                    requiredScore: requiredScore,
                    errors: errors
                });
            }

            return res.status(400).json({
                message: 'Validation failed',
                validationScore: validationScore.toFixed(3),
                requiredScore: requiredScore,
                errors,
                threatLevel: threatLevel !== 'CLEAN' ? threatLevel : undefined
            });
        }

        // Attach sanitized data + meta
        req.validatedData = sanitizedData;
        req.validationMeta = {
            validationScore: validationScore.toFixed(3),
            totalFields,
            validFields,
            threatLevel: threatLevel,
            anomalyScore: totalAnomalyScore,
            attacksBlocked: detectedAttacks.length
        };

        // Add security headers
        res.setHeader('X-Threat-Level', threatLevel);
        res.setHeader('X-Validation-Score', validationScore.toFixed(3));

        next();
    };
};

// ----------------------------
// NEW: Security endpoints for monitoring
// ----------------------------

// Get anomaly statistics (Admin only)
validateInput.getAnomalyStats = () => {
    return anomalyEngine.getStats();
};

// Get threat level for IP (Admin only)
validateInput.getThreatLevel = (ip) => {
    return anomalyEngine.getThreatLevel(ip);
};

// Manually block IP (Admin emergency)
validateInput.blockIp = (ip, reason, duration) => {
    anomalyEngine.blockIp(ip, reason, duration);
};

// Get all blocked IPs (Admin only)
validateInput.getBlockedIPs = () => {
    const blocked = [];
    for (const [ip, expiry] of anomalyEngine.ipBlacklist) {
        blocked.push({
            ip,
            expiresAt: new Date(expiry).toISOString(),
            remainingSeconds: Math.max(0, Math.floor((expiry - Date.now()) / 1000))
        });
    }
    return blocked;
};

module.exports = validateInput;