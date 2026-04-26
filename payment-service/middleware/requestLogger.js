/**
 * ============================================================
 * 📝 REQUEST LOGGER MIDDLEWARE — STRUCTURED OBSERVABILITY v1.0
 * ============================================================
 *
 * PURPOSE:
 * - Structured JSON logging for all HTTP requests
 * - Performance metrics (response time, throughput)
 * - Distributed tracing integration with correlation ID
 * - Privacy-safe logging (PII redaction)
 *
 * SCALE TARGET:
 * - 50M+ requests/day
 * - Sub-millisecond logging overhead
 * - 1% sampling for high-volume endpoints
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: ADAPTIVE SAMPLING (Dynamic Rate Adjustment)
 * ------------------------------------------------------------
 * - Automatically adjusts sampling rate based on system load
 * - High traffic → lower sampling (0.1%), Low traffic → higher sampling (10%)
 * - Preserves critical endpoints (payments, refunds) at 100%
 *
 * 🧠 ALGORITHM 2: PII REDACTION ENGINE (Privacy-First Logging)
 * ------------------------------------------------------------
 * - Automatically detects and redacts sensitive data
 * - Patterns: emails, credit cards, passwords, tokens
 * - Zero-copy redaction for performance
 *
 * ============================================================
 */

const os = require('os');

// ============================================================
// CONFIG
// ============================================================

const DEFAULT_LOG_LEVEL = process.env.LOG_LEVEL || 'info';
const IS_PRODUCTION = process.env.NODE_ENV === 'production';
const SERVICE_NAME = process.env.SERVICE_NAME || 'payment-service';

// Critical endpoints that should ALWAYS be logged (100% sampling)
const CRITICAL_ENDPOINTS = [
    '/api/payments/create-intent',
    '/api/payments/confirm',
    '/api/refund',
    '/api/webhooks/stripe',
];

// Endpoints that can be sampled at lower rate
const HIGH_VOLUME_ENDPOINTS = [
    '/health',
    '/metrics',
    '/api/payments/status',
];

// ============================================================
// 🧠 ALGORITHM 1: ADAPTIVE SAMPLING
// ============================================================

class AdaptiveSampler {
    constructor() {
        this.baseSampleRate = 0.05; // 5% default sampling
        this.currentSampleRate = this.baseSampleRate;
        this.requestCount = 0;
        this.lastAdjustment = Date.now();
        this.adjustmentInterval = 60000; // 1 minute
        this.stats = {
            totalRequests: 0,
            sampledRequests: 0,
            adjustments: 0,
        };

        // Adjust sampling rate every minute
        setInterval(() => this.adjustSamplingRate(), this.adjustmentInterval);
    }

    /**
     * Determine if request should be sampled
     */
    shouldSample(endpoint) {
        this.stats.totalRequests++;

        // Critical endpoints - always sample
        if (CRITICAL_ENDPOINTS.some(e => endpoint.includes(e))) {
            this.stats.sampledRequests++;
            return true;
        }

        // High volume endpoints - use adaptive sampling
        if (HIGH_VOLUME_ENDPOINTS.some(e => endpoint.includes(e))) {
            const shouldSample = Math.random() < this.currentSampleRate;
            if (shouldSample) this.stats.sampledRequests++;
            return shouldSample;
        }

        // Default - sample based on current rate
        const shouldSample = Math.random() < this.currentSampleRate;
        if (shouldSample) this.stats.sampledRequests++;
        return shouldSample;
    }

    /**
     * Dynamically adjust sampling rate based on system load
     */
    adjustSamplingRate() {
        const now = Date.now();
        const requestsPerMinute = this.stats.totalRequests / ((now - this.lastAdjustment) / 60000);

        let newRate = this.currentSampleRate;

        // High traffic (>10k req/min) → reduce sampling
        if (requestsPerMinute > 10000) {
            newRate = Math.max(0.001, this.currentSampleRate * 0.5);
            this.stats.adjustments++;
        }
        // Medium traffic (1k-10k req/min) → moderate sampling
        else if (requestsPerMinute > 1000) {
            newRate = Math.min(0.05, this.currentSampleRate);
        }
        // Low traffic (<1k req/min) → increase sampling
        else if (requestsPerMinute < 1000 && this.currentSampleRate < 0.1) {
            newRate = Math.min(0.1, this.currentSampleRate * 1.5);
            this.stats.adjustments++;
        }

        if (newRate !== this.currentSampleRate) {
            console.log(`[SAMPLER] 📊 Rate adjusted: ${(this.currentSampleRate * 100).toFixed(2)}% → ${(newRate * 100).toFixed(2)}% (${Math.round(requestsPerMinute)} req/min)`);
            this.currentSampleRate = newRate;
        }

        // Reset counters
        this.stats.totalRequests = 0;
        this.lastAdjustment = now;
    }

    getMetrics() {
        return {
            currentSampleRate: (this.currentSampleRate * 100).toFixed(2) + '%',
            totalSampled: this.stats.sampledRequests,
            adjustments: this.stats.adjustments,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: PII REDACTION ENGINE
// ============================================================

class PIIRedactor {
    constructor() {
        // PII patterns with replacement strategies
        this.patterns = [
            {
                name: 'EMAIL',
                pattern: /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g,
                replacement: '[REDACTED_EMAIL]',
            },
            {
                name: 'CREDIT_CARD',
                pattern: /\b(?:\d[ -]*?){13,16}\b/g,
                replacement: '[REDACTED_CARD]',
            },
            {
                name: 'JWT_TOKEN',
                pattern: /eyJ[a-zA-Z0-9_-]*\.eyJ[a-zA-Z0-9_-]*\.[a-zA-Z0-9_-]*/g,
                replacement: '[REDACTED_JWT]',
            },
            {
                name: 'PASSWORD',
                pattern: /"password"\s*:\s*"[^"]*"/gi,
                replacement: '"password":"[REDACTED]"',
            },
            {
                name: 'STRIPE_KEY',
                pattern: /(sk_live|sk_test|pk_live|pk_test)_[a-zA-Z0-9]+/g,
                replacement: '[REDACTED_STRIPE_KEY]',
            },
            {
                name: 'IDEMPOTENCY_KEY',
                pattern: /[a-f0-9]{32,64}/gi,
                replacement: (match) => match.substring(0, 8) + '...[REDACTED]',
            },
        ];

        this.stats = {
            totalRedactions: 0,
            patternsMatched: new Map(),
        };
    }

    /**
     * Redact sensitive data from string
     */
    redact(data) {
        if (!data) return data;
        if (typeof data !== 'string') return data;

        let redacted = data;

        for (const pattern of this.patterns) {
            const matches = redacted.match(pattern.pattern);
            if (matches) {
                this.stats.totalRedactions += matches.length;
                const count = this.stats.patternsMatched.get(pattern.name) || 0;
                this.stats.patternsMatched.set(pattern.name, count + matches.length);

                // Apply replacement
                if (typeof pattern.replacement === 'function') {
                    redacted = redacted.replace(pattern.pattern, pattern.replacement);
                } else {
                    redacted = redacted.replace(pattern.pattern, pattern.replacement);
                }
            }
        }

        return redacted;
    }

    /**
     * Redact object recursively
     */
    redactObject(obj, sensitiveFields = ['password', 'token', 'secret', 'cvv', 'cardNumber']) {
        if (!obj) return obj;
        if (typeof obj !== 'object') return this.redact(String(obj));

        const redacted = Array.isArray(obj) ? [] : {};

        for (const [key, value] of Object.entries(obj)) {
            // Check if field is sensitive
            if (sensitiveFields.some(field => key.toLowerCase().includes(field))) {
                redacted[key] = '[REDACTED]';
                this.stats.totalRedactions++;
            }
            // Recursively redact nested objects
            else if (value && typeof value === 'object') {
                redacted[key] = this.redactObject(value, sensitiveFields);
            }
            // Redact string values
            else if (typeof value === 'string') {
                redacted[key] = this.redact(value);
            }
            else {
                redacted[key] = value;
            }
        }

        return redacted;
    }

    getMetrics() {
        return {
            totalRedactions: this.stats.totalRedactions,
            patternsMatched: Object.fromEntries(this.stats.patternsMatched),
        };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const adaptiveSampler = new AdaptiveSampler();
const piiRedactor = new PIIRedactor();

// ============================================================
// 📊 REQUEST METRICS COLLECTOR
// ============================================================

class RequestMetricsCollector {
    constructor() {
        this.metrics = {
            totalRequests: 0,
            totalErrors: 0,
            avgResponseTime: 0,
            p50ResponseTime: 0,
            p95ResponseTime: 0,
            p99ResponseTime: 0,
            recentResponseTimes: [],
            endpointMetrics: new Map(),
        };

        this.windowSize = 1000;
        this.decayFactor = 0.95;
    }

    recordRequest(endpoint, statusCode, durationMs) {
        this.metrics.totalRequests++;

        if (statusCode >= 400) {
            this.metrics.totalErrors++;
        }

        // Update endpoint-specific metrics
        if (!this.metrics.endpointMetrics.has(endpoint)) {
            this.metrics.endpointMetrics.set(endpoint, {
                count: 0,
                errors: 0,
                totalDuration: 0,
            });
        }

        const endpointMetric = this.metrics.endpointMetrics.get(endpoint);
        endpointMetric.count++;
        if (statusCode >= 400) endpointMetric.errors++;
        endpointMetric.totalDuration += durationMs;

        // Update sliding window for percentiles
        this.metrics.recentResponseTimes.push(durationMs);
        while (this.metrics.recentResponseTimes.length > this.windowSize) {
            this.metrics.recentResponseTimes.shift();
        }

        // Update average with exponential decay
        this.metrics.avgResponseTime =
            this.metrics.avgResponseTime * this.decayFactor +
            durationMs * (1 - this.decayFactor);

        // Calculate percentiles
        const sorted = [...this.metrics.recentResponseTimes].sort((a, b) => a - b);
        if (sorted.length > 0) {
            this.metrics.p50ResponseTime = sorted[Math.floor(sorted.length * 0.5)];
            this.metrics.p95ResponseTime = sorted[Math.floor(sorted.length * 0.95)];
            this.metrics.p99ResponseTime = sorted[Math.floor(sorted.length * 0.99)];
        }
    }

    getMetrics() {
        const topEndpoints = Array.from(this.metrics.endpointMetrics.entries())
            .sort((a, b) => b[1].count - a[1].count)
            .slice(0, 10)
            .map(([name, data]) => ({
                endpoint: name,
                count: data.count,
                errorRate: ((data.errors / data.count) * 100).toFixed(2) + '%',
                avgDurationMs: Math.round(data.totalDuration / data.count),
            }));

        return {
            totalRequests: this.metrics.totalRequests,
            totalErrors: this.metrics.totalErrors,
            errorRate: this.metrics.totalRequests > 0
                ? ((this.metrics.totalErrors / this.metrics.totalRequests) * 100).toFixed(2) + '%'
                : '0%',
            avgResponseTimeMs: Math.round(this.metrics.avgResponseTime),
            p50ResponseTimeMs: this.metrics.p50ResponseTime,
            p95ResponseTimeMs: this.metrics.p95ResponseTime,
            p99ResponseTimeMs: this.metrics.p99ResponseTime,
            topEndpoints,
        };
    }
}

// ============================================================
// 🔧 INITIALIZE METRICS COLLECTOR
// ============================================================

const metricsCollector = new RequestMetricsCollector();

// ============================================================
// 🚀 MAIN REQUEST LOGGER MIDDLEWARE
// ============================================================

const requestLogger = (req, res, next) => {
    const startTime = Date.now();
    const correlationId = req.correlationId || 'no-correlation-id';
    const endpoint = req.path;
    const method = req.method;

    // Determine if we should log this request (adaptive sampling)
    const shouldLog = adaptiveSampler.shouldSample(endpoint);

    // Create log entry structure
    let logEntry = {
        timestamp: new Date().toISOString(),
        level: 'info',
        type: 'REQUEST',
        correlationId,
        service: SERVICE_NAME,
        environment: process.env.NODE_ENV || 'development',
        request: {
            method,
            path: endpoint,
            query: req.query,
            headers: {
                'user-agent': req.headers['user-agent'],
                'content-type': req.headers['content-type'],
                'x-forwarded-for': req.headers['x-forwarded-for'],
            },
            ip: req.ip || req.headers['x-forwarded-for'] || req.socket.remoteAddress,
        },
    };

    // Redact sensitive data from request body (if present and logging)
    if (shouldLog && req.body && Object.keys(req.body).length > 0) {
        logEntry.request.body = piiRedactor.redactObject(req.body);
    }

    // Log request start (debug level, sampled)
    if (shouldLog && DEFAULT_LOG_LEVEL === 'debug') {
        console.log(JSON.stringify({
            ...logEntry,
            level: 'debug',
            message: `→ ${method} ${endpoint}`,
        }));
    }

    // Hook into response to log completion
    const originalJson = res.json.bind(res);
    const originalSend = res.send.bind(res);

    res.json = (body) => {
        res.responseBody = body;
        return originalJson(body);
    };

    res.send = (body) => {
        res.responseBody = body;
        return originalSend(body);
    };

    // Override end to capture completion
    const originalEnd = res.end;
    res.end = function(chunk, encoding) {
        const duration = Date.now() - startTime;
        const statusCode = res.statusCode;

        // Record metrics
        metricsCollector.recordRequest(endpoint, statusCode, duration);

        // Build completion log entry
        const completionLog = {
            ...logEntry,
            level: statusCode >= 400 ? 'error' : 'info',
            message: `${method} ${endpoint} ${statusCode} ${duration}ms`,
            response: {
                statusCode,
                durationMs: duration,
            },
        };

        // Add error details if present
        if (statusCode >= 400 && res.responseBody) {
            const errorBody = piiRedactor.redactObject(res.responseBody);
            completionLog.response.error = errorBody;
            completionLog.level = statusCode >= 500 ? 'error' : 'warn';
        }

        // Add performance metrics for slow requests
        if (duration > 1000) {
            completionLog.performance = {
                slow: true,
                thresholdMs: 1000,
                suggestion: duration > 3000 ? 'Investigate performance issue' : 'Monitor',
            };
        }

        // Log completion (sampled or always for errors)
        if (shouldLog || statusCode >= 400) {
            console.log(JSON.stringify(completionLog));
        }

        originalEnd.call(this, chunk, encoding);
    };

    next();
};

// ============================================================
// 📊 METRICS ENDPOINT HELPER
// ============================================================

const getRequestLoggerMetrics = () => {
    return {
        sampler: adaptiveSampler.getMetrics(),
        redactor: piiRedactor.getMetrics(),
        requestMetrics: metricsCollector.getMetrics(),
    };
};

// ============================================================
// 🧠 INNOVATION: HEALTH CHECK FOR LOGGER
// ============================================================

const loggerHealthCheck = () => {
    const metrics = metricsCollector.getMetrics();
    const samplerMetrics = adaptiveSampler.getMetrics();

    let status = 'HEALTHY';
    if (metrics.errorRate > 5) status = 'DEGRADED';
    if (metrics.totalRequests > 0 && metrics.avgResponseTimeMs > 5000) status = 'CRITICAL';

    return {
        status,
        timestamp: new Date().toISOString(),
        metrics: {
            errorRate: metrics.errorRate,
            avgResponseTime: metrics.avgResponseTimeMs,
            sampleRate: samplerMetrics.currentSampleRate,
        },
    };
};

// ============================================================
// EXPORTS
// ============================================================

module.exports = {
    // Main middleware
    requestLogger,

    // Metrics and health
    getRequestLoggerMetrics,
    loggerHealthCheck,

    // Individual components for advanced use
    adaptiveSampler,
    piiRedactor,
    metricsCollector,
};