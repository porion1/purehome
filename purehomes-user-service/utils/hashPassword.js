const bcrypt = require('bcrypt');
const crypto = require('crypto');
const os = require('os');

/**
 * INNOVATION ALGORITHM: Adaptive Password Strength Scoring with Progressive Hashing (APS-PH)
 *
 * This algorithm dynamically adjusts password security based on:
 * 1. Real-time system load (CPU availability)
 * 2. Password entropy analysis (character diversity)
 * 3. Pattern-based weakness detection (sequential, repetitive, common patterns)
 * 4. Progressive hash rounds scaling (10-14 rounds based on strength)
 *
 * Unlike static bcrypt implementations, APS-PH optimizes between security and performance,
 * applying stronger hashing to weak passwords while using fewer resources for strong ones.
 */
class AdaptivePasswordHasher {
    constructor() {
        this.baseRounds = 12;
        this.maxRounds = 14;
        this.minRounds = 10;

        // Pre-compiled patterns for performance
        this.commonPasswords = new Set([
            'password', '123456', 'qwerty', 'admin', 'letmein', 'welcome',
            'monkey', 'dragon', 'master', 'sunshine', 'passw0rd', 'password123'
        ]);
        this.repetitionPattern = /(.)\1{2,}/;
        this.sequentialPattern = /(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz|123|234|345|456|567|678|789|890)/i;
        this.keyboardPattern = /(qwerty|asdfgh|zxcvbn|qwertyuiop|asdfghjkl|zxcvbnm)/i;

        // Metrics for monitoring (no console logs)
        this.metrics = {
            totalHashes: 0,
            averageTime: 0,
            weakPasswordsRejected: 0
        };
    }

    /**
     * Calculate password entropy (bits)
     */
    calculateEntropy(password) {
        let charset = 0;
        if (/[a-z]/.test(password)) charset += 26;
        if (/[A-Z]/.test(password)) charset += 26;
        if (/[0-9]/.test(password)) charset += 10;
        if (/[^a-zA-Z0-9]/.test(password)) charset += 33;

        return password.length * Math.log2(charset || 1);
    }

    /**
     * Detect pattern-based weaknesses
     */
    detectWeaknesses(password) {
        let score = 100;

        // Check against common passwords
        if (this.commonPasswords.has(password.toLowerCase())) {
            return { score: 0, isWeak: true };
        }

        // Check for repeated characters
        if (this.repetitionPattern.test(password)) {
            score -= 25;
        }

        // Check for sequential patterns
        if (this.sequentialPattern.test(password)) {
            score -= 30;
        }

        // Check for keyboard patterns
        if (this.keyboardPattern.test(password.toLowerCase())) {
            score -= 35;
        }

        // Length-based scoring
        if (password.length < 8) {
            score -= 50;
        } else if (password.length < 10) {
            score -= 20;
        } else if (password.length >= 16) {
            score += 10;
        }

        // Character diversity
        const hasLower = /[a-z]/.test(password);
        const hasUpper = /[A-Z]/.test(password);
        const hasNumber = /[0-9]/.test(password);
        const hasSpecial = /[^a-zA-Z0-9]/.test(password);

        const diversityCount = [hasLower, hasUpper, hasNumber, hasSpecial].filter(Boolean).length;
        score += (diversityCount - 2) * 15;

        // Entropy bonus
        const entropy = this.calculateEntropy(password);
        score += Math.min(entropy / 5, 20);

        return {
            score: Math.max(0, Math.min(100, score)),
            isWeak: score < 50
        };
    }

    /**
     * Calculate optimal hash rounds based on password strength and system load
     */
    async calculateOptimalRounds(passwordStrength) {
        // FIX: os.loadavg() returns [0,0,0] on Windows, handle gracefully
        let systemLoadPercent = 0;
        try {
            const loadAvg = os.loadavg()[0];
            const cpuCores = os.cpus().length;
            systemLoadPercent = cpuCores > 0 ? loadAvg / cpuCores : 0;
        } catch (err) {
            systemLoadPercent = 0;
        }

        let rounds = this.baseRounds;

        // Adjust based on password strength
        if (passwordStrength.score >= 80) {
            rounds = this.minRounds;
        } else if (passwordStrength.score >= 50) {
            rounds = this.baseRounds;
        } else {
            rounds = this.maxRounds;
        }

        // Reduce rounds under high load (protect system)
        if (systemLoadPercent > 0.8) {
            rounds = Math.max(this.minRounds, rounds - 2);
        } else if (systemLoadPercent > 0.7) {
            rounds = Math.max(this.minRounds, rounds - 1);
        }

        return Math.min(this.maxRounds, Math.max(this.minRounds, rounds));
    }

    /**
     * Generate salt
     */
    async generateSalt(rounds) {
        return bcrypt.genSalt(rounds);
    }

    /**
     * Hash password with adaptive security
     */
    async hashPassword(password) {
        const startTime = Date.now();

        // Strict validation
        if (!password || typeof password !== 'string') {
            throw new Error('Password must be a non-empty string');
        }

        if (password.length < 8) {
            throw new Error('Password must be at least 8 characters');
        }

        if (password.length > 128) {
            throw new Error('Password exceeds maximum length of 128 characters');
        }

        // Analyze password strength
        const strengthAnalysis = this.detectWeaknesses(password);

        // Reject weak passwords
        if (strengthAnalysis.isWeak) {
            this.metrics.weakPasswordsRejected++;
            throw new Error('Password is too weak. Use a stronger password with mixed case, numbers, and longer length.');
        }

        // Calculate optimal rounds
        const optimalRounds = await this.calculateOptimalRounds(strengthAnalysis);

        // Generate salt and hash
        const salt = await this.generateSalt(optimalRounds);
        const hash = await bcrypt.hash(password, salt);

        // Update metrics
        const elapsed = Date.now() - startTime;
        this.metrics.totalHashes++;
        this.metrics.averageTime = (this.metrics.averageTime * (this.metrics.totalHashes - 1) + elapsed) / this.metrics.totalHashes;

        return hash;
    }

    /**
     * Compare password with timing attack protection
     */
    async comparePassword(password, hashedPassword) {
        if (!password || !hashedPassword || typeof password !== 'string') {
            return false;
        }

        try {
            return await bcrypt.compare(password, hashedPassword);
        } catch (error) {
            return false;
        }
    }

    /**
     * Validate password against security policy
     */
    validatePasswordPolicy(password) {
        const errors = [];

        if (!password || password.length < 8) {
            errors.push('Password must be at least 8 characters');
        }

        if (!/[A-Z]/.test(password)) {
            errors.push('Password must contain at least one uppercase letter');
        }

        if (!/[a-z]/.test(password)) {
            errors.push('Password must contain at least one lowercase letter');
        }

        if (!/[0-9]/.test(password)) {
            errors.push('Password must contain at least one number');
        }

        const weaknessAnalysis = this.detectWeaknesses(password);
        if (weaknessAnalysis.isWeak) {
            errors.push('Password is too weak (common pattern detected)');
        }

        return {
            isValid: errors.length === 0,
            errors
        };
    }

    /**
     * Get metrics for monitoring
     */
    getMetrics() {
        return {
            totalHashes: this.metrics.totalHashes,
            averageTimeMs: Math.round(this.metrics.averageTime),
            weakPasswordsRejected: this.metrics.weakPasswordsRejected
        };
    }
}

// Singleton instance
const passwordHasher = new AdaptivePasswordHasher();

/**
 * Hash password with adaptive algorithm
 */
const hashPassword = async (password) => {
    return passwordHasher.hashPassword(password);
};

/**
 * Compare password with hashed password
 */
const comparePassword = async (password, hashedPassword) => {
    return passwordHasher.comparePassword(password, hashedPassword);
};

/**
 * Validate password strength
 */
const validatePassword = (password) => {
    return passwordHasher.validatePasswordPolicy(password);
};

/**
 * Get hasher metrics for monitoring
 */
const getPasswordMetrics = () => {
    return passwordHasher.getMetrics();
};

module.exports = {
    hashPassword,
    comparePassword,
    validatePassword,
    getPasswordMetrics
};