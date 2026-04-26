/**
 * ============================================================
 * 🚨 ERROR MIDDLEWARE — GLOBAL ERROR HANDLER v1.0
 * ============================================================
 *
 * PURPOSE:
 * - Centralized error handling for entire payment service
 * - Standardized error response format across all endpoints
 * - Automatic error classification and prioritization
 * - Integration with correlation ID for distributed tracing
 *
 * SCALE TARGET:
 * - 50M+ requests
 * - <1ms error processing overhead
 * - 100% error capture with sampling for logs
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: ERROR CLASSIFICATION MATRIX (ECM)
 * ------------------------------------------------------------
 * - Auto-classifies errors by type, severity, and source
 * - Maps error codes to user-friendly messages
 * - Determines retryability and HTTP status codes
 *
 * 🧠 ALGORITHM 2: CIRCUIT-AWARE ERROR ESCALATION (CAEE)
 * ------------------------------------------------------------
 * - Detects error patterns that indicate circuit breaker events
 * - Auto-escalates critical errors to alerting systems
 * - Prevents error storms with intelligent throttling
 *
 * ============================================================
 */

const os = require('os');

// ============================================================
// CONFIG
// ============================================================

const IS_PRODUCTION = process.env.NODE_ENV === 'production';
const SERVICE_NAME = process.env.SERVICE_NAME || 'payment-service';
const ERROR_LOG_SAMPLE_RATE = 0.1; // 10% of errors logged in production

// ============================================================
// 🧠 ALGORITHM 1: ERROR CLASSIFICATION MATRIX (ECM)
// ============================================================

class ErrorClassifier {
    constructor() {
        // Error classification matrix
        this.classificationMap = new Map();
        this.severityLevels = {
            LOW: 1,
            MEDIUM: 2,
            HIGH: 3,
            CRITICAL: 4,
        };

        this.stats = {
            totalErrors: 0,
            classifiedErrors: new Map(), // severity -> count
            topErrorTypes: new Map(),
        };

        this.initializeClassificationMap();
    }

    initializeClassificationMap() {
        // Payment-specific errors
        this.classificationMap.set('StripeCardError', {
            category: 'PAYMENT_DECLINED',
            severity: 'MEDIUM',
            httpStatus: 402,
            userMessage: 'Your card was declined. Please use a different payment method.',
            retryable: false,
            shouldAlert: false,
        });

        this.classificationMap.set('StripeRateLimitError', {
            category: 'RATE_LIMIT',
            severity: 'MEDIUM',
            httpStatus: 429,
            userMessage: 'Too many requests. Please try again later.',
            retryable: true,
            shouldAlert: false,
        });

        this.classificationMap.set('StripeAPIError', {
            category: 'EXTERNAL_API_ERROR',
            severity: 'HIGH',
            httpStatus: 502,
            userMessage: 'Payment provider temporarily unavailable. Please try again.',
            retryable: true,
            shouldAlert: true,
        });

        this.classificationMap.set('StripeConnectionError', {
            category: 'NETWORK_ERROR',
            severity: 'HIGH',
            httpStatus: 503,
            userMessage: 'Network error. Please check your connection and try again.',
            retryable: true,
            shouldAlert: true,
        });

        // Validation errors
        this.classificationMap.set('ValidationError', {
            category: 'VALIDATION_ERROR',
            severity: 'LOW',
            httpStatus: 400,
            userMessage: 'Invalid request data. Please check your input.',
            retryable: false,
            shouldAlert: false,
        });

        // Auth errors
        this.classificationMap.set('UnauthorizedError', {
            category: 'AUTH_ERROR',
            severity: 'MEDIUM',
            httpStatus: 401,
            userMessage: 'Authentication required. Please log in.',
            retryable: false,
            shouldAlert: false,
        });

        this.classificationMap.set('ForbiddenError', {
            category: 'AUTH_ERROR',
            severity: 'HIGH',
            httpStatus: 403,
            userMessage: 'You do not have permission to perform this action.',
            retryable: false,
            shouldAlert: true,
        });

        // Idempotency errors
        this.classificationMap.set('IdempotencyError', {
            category: 'IDEMPOTENCY_ERROR',
            severity: 'LOW',
            httpStatus: 409,
            userMessage: 'Duplicate request detected.',
            retryable: false,
            shouldAlert: false,
        });

        // Database errors
        this.classificationMap.set('MongoError', {
            category: 'DATABASE_ERROR',
            severity: 'HIGH',
            httpStatus: 503,
            userMessage: 'Service temporarily unavailable. Please try again.',
            retryable: true,
            shouldAlert: true,
        });

        // Circuit breaker errors
        this.classificationMap.set('CircuitOpenError', {
            category: 'CIRCUIT_OPEN',
            severity: 'CRITICAL',
            httpStatus: 503,
            userMessage: 'Service is recovering. Please try again in a few moments.',
            retryable: true,
            shouldAlert: true,
        });

        // Default fallback
        this.classificationMap.set('default', {
            category: 'UNKNOWN_ERROR',
            severity: 'MEDIUM',
            httpStatus: 500,
            userMessage: 'An unexpected error occurred. Please try again.',
            retryable: false,
            shouldAlert: true,
        });
    }

    classify(error) {
        this.stats.totalErrors++;

        // Determine error type
        let errorType = error.name || error.code || error.type || 'UnknownError';

        // Check for specific Stripe error types
        if (error.type && error.type.startsWith('Stripe')) {
            errorType = error.type;
        }

        // Get classification
        const classification = this.classificationMap.get(errorType) ||
            this.classificationMap.get('default');

        // Update stats
        const severityCount = this.stats.classifiedErrors.get(classification.severity) || 0;
        this.stats.classifiedErrors.set(classification.severity, severityCount + 1);

        const errorTypeCount = this.stats.topErrorTypes.get(errorType) || 0;
        this.stats.topErrorTypes.set(errorType, errorTypeCount + 1);

        // Add additional metadata
        return {
            ...classification,
            originalType: errorType,
            originalMessage: error.message,
            stack: IS_PRODUCTION ? undefined : error.stack,
            code: error.code,
            param: error.param,
            declineCode: error.decline_code,
        };
    }

    getMetrics() {
        const topErrors = Array.from(this.stats.topErrorTypes.entries())
            .sort((a, b) => b[1] - a[1])
            .slice(0, 10)
            .map(([type, count]) => ({ type, count }));

        return {
            totalErrors: this.stats.totalErrors,
            severityDistribution: Object.fromEntries(this.stats.classifiedErrors),
            topErrorTypes: topErrors,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: CIRCUIT-AWARE ERROR ESCALATION (CAEE)
// ============================================================

class ErrorEscalator {
    constructor() {
        this.errorHistory = []; // Rolling window of errors
        this.windowSize = 1000;
        this.windowTimeMs = 60000; // 1 minute window
        this.alertThresholds = {
            consecutiveFailures: 10,
            errorRate: 0.2, // 20% error rate triggers alert
            criticalErrors: 5,
        };
        this.lastAlertTime = 0;
        this.minAlertIntervalMs = 30000; // 30 seconds between alerts
        this.stats = {
            totalEscalations: 0,
            alertsSent: 0,
            circuitEvents: 0,
        };

        // Cleanup old errors every minute
        setInterval(() => this.cleanupErrorHistory(), 60000);
    }

    /**
     * Record error and determine if escalation needed
     */
    recordError(error, classification, req) {
        const now = Date.now();

        // Add to history
        this.errorHistory.push({
            timestamp: now,
            error: classification,
            endpoint: req?.path,
            correlationId: req?.correlationId,
        });

        // Trim history
        while (this.errorHistory.length > this.windowSize) {
            this.errorHistory.shift();
        }

        // Check if escalation needed
        const escalation = this.checkEscalationNeeded(classification, req);

        if (escalation.shouldEscalate) {
            this.stats.totalEscalations++;
            this.escalate(escalation, classification, req);
        }

        return escalation;
    }

    /**
     * Check if error should be escalated
     */
    checkEscalationNeeded(classification, req) {
        const now = Date.now();
        const recentErrors = this.errorHistory.filter(e => now - e.timestamp < this.windowTimeMs);
        const recentCount = recentErrors.length;

        // Check for consecutive failures on same endpoint
        if (req) {
            const endpointErrors = recentErrors.filter(e => e.endpoint === req.path);
            if (endpointErrors.length >= this.alertThresholds.consecutiveFailures) {
                return {
                    shouldEscalate: true,
                    reason: 'CONSECUTIVE_FAILURES',
                    count: endpointErrors.length,
                    endpoint: req.path,
                };
            }
        }

        // Check error rate
        const errorRate = recentCount / this.windowSize;
        if (errorRate >= this.alertThresholds.errorRate && recentCount > 50) {
            return {
                shouldEscalate: true,
                reason: 'HIGH_ERROR_RATE',
                rate: (errorRate * 100).toFixed(1) + '%',
                count: recentCount,
            };
        }

        // Check for critical errors
        const criticalErrors = recentErrors.filter(e => e.error.severity === 'CRITICAL');
        if (criticalErrors.length >= this.alertThresholds.criticalErrors) {
            return {
                shouldEscalate: true,
                reason: 'CRITICAL_ERRORS',
                count: criticalErrors.length,
                errors: criticalErrors.map(e => e.error.category),
            };
        }

        // Individual critical error
        if (classification.severity === 'CRITICAL') {
            return {
                shouldEscalate: true,
                reason: 'CRITICAL_ERROR_SINGLE',
                category: classification.category,
            };
        }

        return { shouldEscalate: false };
    }

    /**
     * Escalate error to alerting system
     */
    escalate(escalation, classification, req) {
        const now = Date.now();

        // Rate limit alerts
        if (now - this.lastAlertTime < this.minAlertIntervalMs) {
            return;
        }

        this.lastAlertTime = now;
        this.stats.alertsSent++;

        if (classification.category === 'CIRCUIT_OPEN') {
            this.stats.circuitEvents++;
        }

        // Build alert payload
        const alert = {
            type: 'ERROR_ESCALATION',
            severity: classification.severity,
            reason: escalation.reason,
            service: SERVICE_NAME,
            timestamp: new Date().toISOString(),
            hostname: os.hostname(),
            pid: process.pid,
            details: {
                category: classification.category,
                originalType: classification.originalType,
                message: classification.originalMessage,
                endpoint: req?.path,
                correlationId: req?.correlationId,
                escalation: escalation,
            },
        };

        // Log alert (in production, send to PagerDuty/Slack/DataDog)
        console.error(JSON.stringify({
            type: 'ALERT',
            ...alert,
        }));

        // In production, you would:
        // - Send to PagerDuty: pagerduty.incident.create(alert)
        // - Send to Slack: slack.webhook.send(alert)
        // - Send to DataDog: datadog.event.create(alert)
    }

    cleanupErrorHistory() {
        const now = Date.now();
        const cutoff = now - this.windowTimeMs;
        const beforeCount = this.errorHistory.length;
        this.errorHistory = this.errorHistory.filter(e => e.timestamp > cutoff);

        if (beforeCount !== this.errorHistory.length) {
            console.log(`[ESCALATOR] 🧹 Cleaned ${beforeCount - this.errorHistory.length} old errors`);
        }
    }

    getMetrics() {
        const recentCount = this.errorHistory.filter(
            e => Date.now() - e.timestamp < this.windowTimeMs
        ).length;

        return {
            totalEscalations: this.stats.totalEscalations,
            alertsSent: this.stats.alertsSent,
            circuitEvents: this.stats.circuitEvents,
            recentErrorCount: recentCount,
            errorHistorySize: this.errorHistory.length,
        };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const errorClassifier = new ErrorClassifier();
const errorEscalator = new ErrorEscalator();

// ============================================================
// 🚀 MAIN ERROR HANDLING MIDDLEWARE
// ============================================================

/**
 * Not found handler - for routes that don't exist
 */
const notFoundHandler = (req, res, next) => {
    const error = new Error(`Route not found: ${req.method} ${req.path}`);
    error.name = 'NotFoundError';
    error.statusCode = 404;
    next(error);
};

/**
 * Global error handler middleware
 */
const errorHandler = (err, req, res, next) => {
    // Set default values
    err.statusCode = err.statusCode || err.status || 500;
    err.name = err.name || err.code || 'InternalServerError';

    // Classify error
    const classification = errorClassifier.classify(err);

    // Record and check escalation
    const escalation = errorEscalator.recordError(err, classification, req);

    // Determine if error should be logged (sampling in production)
    const shouldLog = !IS_PRODUCTION || Math.random() < ERROR_LOG_SAMPLE_RATE;

    if (shouldLog || classification.severity === 'CRITICAL' || err.statusCode >= 500) {
        // Build error log entry
        const errorLog = {
            timestamp: new Date().toISOString(),
            level: err.statusCode >= 500 ? 'error' : 'warn',
            type: 'ERROR',
            correlationId: req.correlationId || 'no-correlation-id',
            service: SERVICE_NAME,
            environment: process.env.NODE_ENV || 'development',
            error: {
                name: classification.originalType,
                message: classification.originalMessage,
                category: classification.category,
                severity: classification.severity,
                statusCode: classification.httpStatus,
                retryable: classification.retryable,
                stack: classification.stack,
                code: classification.code,
                param: classification.param,
                declineCode: classification.declineCode,
            },
            request: {
                method: req.method,
                path: req.path,
                query: req.query,
                ip: req.ip || req.headers['x-forwarded-for'] || req.socket.remoteAddress,
                userAgent: req.headers['user-agent'],
            },
            escalation: escalation.shouldEscalate ? escalation : undefined,
        };

        // Log error
        console.error(JSON.stringify(errorLog));
    }

    // Build response body
    const responseBody = {
        success: false,
        error: {
            code: classification.category,
            message: classification.userMessage,
            statusCode: classification.httpStatus,
        },
        timestamp: new Date().toISOString(),
        correlationId: req.correlationId,
    };

    // Add retry-after header for rate limit errors
    if (classification.category === 'RATE_LIMIT') {
        res.setHeader('Retry-After', '60');
    }

    // Add additional details in development
    if (!IS_PRODUCTION) {
        responseBody.error.details = {
            originalError: classification.originalMessage,
            errorType: classification.originalType,
            stack: classification.stack,
        };
    }

    // Add idempotency info if available
    if (req.idempotencyKey) {
        responseBody.idempotencyKey = req.idempotencyKey;
    }

    // Send response
    res.status(classification.httpStatus).json(responseBody);
};

// ============================================================
// 📊 HELPER FUNCTIONS FOR METRICS
// ============================================================

/**
 * Get error metrics for monitoring
 */
const getErrorMetrics = () => {
    return {
        classifier: errorClassifier.getMetrics(),
        escalator: errorEscalator.getMetrics(),
    };
};

/**
 * Health check for error handler
 */
const errorHandlerHealthCheck = () => {
    const metrics = errorClassifier.getMetrics();
    const escalatorMetrics = errorEscalator.getMetrics();

    let status = 'HEALTHY';
    if (escalatorMetrics.recentErrorCount > 100) status = 'DEGRADED';
    if (escalatorMetrics.circuitEvents > 0) status = 'DEGRADED';

    return {
        status,
        timestamp: new Date().toISOString(),
        metrics: {
            totalErrors: metrics.totalErrors,
            recentErrors: escalatorMetrics.recentErrorCount,
            alertsSent: escalatorMetrics.alertsSent,
        },
    };
};

/**
 * Reset error handler state (for testing)
 */
const resetErrorHandler = () => {
    // This would reset internal state - for testing only
    console.warn('[ERROR-HANDLER] Reset called - only for testing');
};

// ============================================================
// EXPORTS
// ============================================================

module.exports = {
    // Main middleware
    notFoundHandler,
    errorHandler,

    // Metrics and health
    getErrorMetrics,
    errorHandlerHealthCheck,
    resetErrorHandler,

    // Individual components for advanced use
    errorClassifier,
    errorEscalator,
};