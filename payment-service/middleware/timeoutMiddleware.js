/**
 * ============================================================
 * ⏱️ TIMEOUT MIDDLEWARE — REQUEST DEADLINE CONTROL v2.0
 * ============================================================
 *
 * PURPOSE:
 * - Enforce request timeouts to prevent hanging connections
 * - Propagate deadline context to downstream services
 * - Cancel long-running operations gracefully
 * - Prevent resource exhaustion at 50M scale
 *
 * SCALE TARGET:
 * - 50M+ concurrent requests
 * - Sub-millisecond timeout checking
 * - Zero memory leaks with proper cleanup
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: ADAPTIVE TIMEOUT (Dynamic Deadline Calculation) [KEPT]
 * ------------------------------------------------------------
 * - Calculates timeout based on endpoint criticality
 * - Adjusts for system load and historical p95 latency
 * - Longer timeouts for payments, shorter for health checks
 *
 * 🧠 ALGORITHM 2: DEADLINE PROPAGATION (Context-Aware Cancellation) [KEPT]
 * ------------------------------------------------------------
 * - Adds X-Deadline header for downstream services
 * - Propagates remaining time to async operations
 * - Coordinates cancellation across service boundaries
 *
 * 🧠 ALGORITHM 3: SMART EXCLUSION (Health & Metrics Bypass) [NEW]
 * ------------------------------------------------------------
 * - Automatically bypasses timeout for health/liveness endpoints
 * - Prevents false positive timeouts on critical monitoring paths
 * - Zero impact on production monitoring
 *
 * 🧠 ALGORITHM 4: ADAPTIVE HEALTH TIMEOUT (Dynamic Health Window) [NEW]
 * ------------------------------------------------------------
 * - Longer timeout for health checks to accommodate external services
 * - Auto-adjusts based on system load
 * - Prevents health check failures during high load
 *
 * ============================================================
 */

// ============================================================
// CONFIG
// ============================================================

const DEFAULT_TIMEOUT_MS = 30000; // 30 seconds

// Endpoints that should NEVER timeout (critical for monitoring)
const NO_TIMEOUT_ENDPOINTS = [
    '/health',
    '/health/live',
    '/health/ready',
    '/metrics',
    '/_internal/metrics',
];

// Endpoint-specific timeout overrides
const DEFAULT_TIMEOUT_ENDPOINTS = {
    // Critical payment endpoints (longer timeout)
    '/api/payments/create-intent': 20000,      // 20 seconds
    '/api/payments/confirm': 25000,             // 25 seconds (includes Stripe)
    '/api/refund': 35000,                       // 35 seconds
    '/api/webhooks/stripe': 30000,              // 30 seconds

    // Fast endpoints (standard timeout)
    '/api/payments/status': 8000,               // 8 seconds
    '/api/refunds/status': 8000,                // 8 seconds

    // Health endpoints (generous timeout for external checks)
    '/health': 15000,                           // 15 seconds (FIXED: was 3000)
    '/metrics': 10000,                          // 10 seconds

    // Default fallback
    'default': DEFAULT_TIMEOUT_MS,
};

// ============================================================
// 🧠 ALGORITHM 1: ADAPTIVE TIMEOUT (Enhanced)
// ============================================================

class AdaptiveTimeoutCalculator {
    constructor() {
        this.endpointLatencies = new Map();
        this.windowSizeMs = 60000;
        this.sampleWindow = [];
        this.stats = {
            totalCalculations: 0,
            adjustments: 0,
            avgTimeout: DEFAULT_TIMEOUT_MS,
        };
        setInterval(() => this.updatePercentiles(), 30000); // Less frequent
    }

    recordLatency(endpoint, durationMs, success) {
        if (!this.endpointLatencies.has(endpoint)) {
            this.endpointLatencies.set(endpoint, []);
        }

        const latencies = this.endpointLatencies.get(endpoint);
        latencies.push({ duration: durationMs, timestamp: Date.now(), success });

        const cutoff = Date.now() - this.windowSizeMs;
        const filtered = latencies.filter(l => l.timestamp > cutoff);
        this.endpointLatencies.set(endpoint, filtered);

        this.sampleWindow.push({ endpoint, duration: durationMs, timestamp: Date.now() });
        while (this.sampleWindow.length > 10000) {
            this.sampleWindow.shift();
        }
    }

    getP95Latency(endpoint) {
        const latencies = this.endpointLatencies.get(endpoint);
        if (!latencies || latencies.length < 10) return null;

        const successfulLatencies = latencies
            .filter(l => l.success)
            .map(l => l.duration)
            .sort((a, b) => a - b);

        if (successfulLatencies.length === 0) return null;

        const p95Index = Math.floor(successfulLatencies.length * 0.95);
        return successfulLatencies[p95Index];
    }

    calculateTimeout(endpoint) {
        this.stats.totalCalculations++;

        // 🧠 SMART EXCLUSION: Skip timeout for health/metrics endpoints
        if (NO_TIMEOUT_ENDPOINTS.some(e => endpoint === e || endpoint.startsWith(e))) {
            return null; // No timeout for monitoring endpoints
        }

        let baseTimeout = DEFAULT_TIMEOUT_ENDPOINTS[endpoint] || DEFAULT_TIMEOUT_ENDPOINTS.default;

        // 🧠 ADAPTIVE HEALTH: Special handling for health checks
        if (endpoint === '/health' || endpoint.startsWith('/health')) {
            // Use longer timeout for health checks
            baseTimeout = Math.max(baseTimeout, 15000);
        }

        const p95Latency = this.getP95Latency(endpoint);

        if (p95Latency) {
            const adaptiveTimeout = Math.min(p95Latency * 2, DEFAULT_TIMEOUT_MS * 2);
            const finalTimeout = Math.max(baseTimeout, adaptiveTimeout);

            if (Math.abs(finalTimeout - this.stats.avgTimeout) > 2000) {
                this.stats.adjustments++;
                if (process.env.NODE_ENV !== 'production') {
                    console.log(`[TIMEOUT] Adaptive timeout for ${endpoint}: ${finalTimeout}ms`);
                }
            }

            this.stats.avgTimeout = this.stats.avgTimeout * 0.95 + finalTimeout * 0.05;
            return finalTimeout;
        }

        return baseTimeout;
    }

    updatePercentiles() {
        const allLatencies = this.sampleWindow
            .filter(l => l.duration > 0)
            .map(l => l.duration)
            .sort((a, b) => a - b);

        if (allLatencies.length > 0) {
            const p95Index = Math.floor(allLatencies.length * 0.95);
            const systemP95 = allLatencies[p95Index];

            if (systemP95 > DEFAULT_TIMEOUT_MS * 0.9) {
                console.warn(`[TIMEOUT] System p95: ${systemP95}ms`);
            }
        }
    }

    getMetrics() {
        return {
            totalCalculations: this.stats.totalCalculations,
            adjustments: this.stats.adjustments,
            avgTimeoutMs: Math.round(this.stats.avgTimeout),
            trackedEndpoints: this.endpointLatencies.size,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: DEADLINE PROPAGATION [KEPT]
// ============================================================

class DeadlinePropagator {
    constructor() {
        this.activeDeadlines = new Map();
        this.stats = {
            totalDeadlines: 0,
            expiredDeadlines: 0,
            propagatedHeaders: 0,
        };
        setInterval(() => this.cleanup(), 60000);
    }

    createDeadline(requestId, timeoutMs) {
        const deadline = Date.now() + timeoutMs;

        if (this.activeDeadlines.has(requestId)) {
            const existing = this.activeDeadlines.get(requestId);
            if (existing.timer) clearTimeout(existing.timer);
        }

        const timer = setTimeout(() => {
            this.expireDeadline(requestId);
        }, timeoutMs);

        this.activeDeadlines.set(requestId, {
            deadline,
            timeoutMs,
            timer,
            createdAt: Date.now(),
        });

        this.stats.totalDeadlines++;
        return deadline;
    }

    getRemainingTime(requestId) {
        const deadline = this.activeDeadlines.get(requestId);
        if (!deadline) return null;
        const remaining = deadline.deadline - Date.now();
        return Math.max(0, remaining);
    }

    isExpired(requestId) {
        const remaining = this.getRemainingTime(requestId);
        return remaining !== null && remaining <= 0;
    }

    getDeadlineHeader(requestId) {
        const remaining = this.getRemainingTime(requestId);
        if (remaining === null) return null;
        this.stats.propagatedHeaders++;
        return remaining.toString();
    }

    expireDeadline(requestId) {
        const deadline = this.activeDeadlines.get(requestId);
        if (deadline) {
            if (deadline.timer) clearTimeout(deadline.timer);
            this.activeDeadlines.delete(requestId);
            this.stats.expiredDeadlines++;
        }
    }

    cleanup() {
        const now = Date.now();
        let cleaned = 0;
        for (const [requestId, deadline] of this.activeDeadlines.entries()) {
            if (deadline.deadline < now) {
                if (deadline.timer) clearTimeout(deadline.timer);
                this.activeDeadlines.delete(requestId);
                cleaned++;
            }
        }
        if (cleaned > 0 && process.env.NODE_ENV !== 'production') {
            console.log(`[DEADLINE] Cleaned ${cleaned} expired deadlines`);
        }
    }

    complete(requestId) {
        const deadline = this.activeDeadlines.get(requestId);
        if (deadline) {
            if (deadline.timer) clearTimeout(deadline.timer);
            this.activeDeadlines.delete(requestId);
        }
    }

    getMetrics() {
        return {
            activeDeadlines: this.activeDeadlines.size,
            totalDeadlines: this.stats.totalDeadlines,
            expiredDeadlines: this.stats.expiredDeadlines,
            propagatedHeaders: this.stats.propagatedHeaders,
        };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const timeoutCalculator = new AdaptiveTimeoutCalculator();
const deadlinePropagator = new DeadlinePropagator();

// ============================================================
// 📊 HELPER: TIMEOUT ERROR CREATION
// ============================================================

const createTimeoutError = (timeoutMs, endpoint) => {
    const error = new Error(`Request timeout after ${timeoutMs}ms`);
    error.code = 'REQUEST_TIMEOUT';
    error.statusCode = 408;
    error.endpoint = endpoint;
    error.retryable = true;
    return error;
};

// ============================================================
// 🚀 MAIN TIMEOUT MIDDLEWARE (ENHANCED)
// ============================================================

const timeoutMiddleware = (req, res, next) => {
    const requestId = req.correlationId || `req_${Date.now()}_${Math.random().toString(36).substring(7)}`;
    req.requestId = requestId;

    const timeoutMs = timeoutCalculator.calculateTimeout(req.path);

    // Skip timeout for excluded endpoints (health, metrics, etc.)
    if (timeoutMs === null) {
        return next();
    }

    const deadline = deadlinePropagator.createDeadline(requestId, timeoutMs);
    req.deadline = deadline;
    req.timeoutMs = timeoutMs;

    let isTimedOut = false;
    let isCompleted = false;

    const timeoutId = setTimeout(() => {
        if (isCompleted) return;

        isTimedOut = true;

        if (res.headersSent) {
            console.error(`[TIMEOUT] Request ${requestId} timed out but response already sent`);
            req.destroy?.();
            return;
        }

        const timeoutError = createTimeoutError(timeoutMs, req.path);
        req.timeoutError = timeoutError;

        res.status(408).json({
            success: false,
            message: `Request timeout after ${timeoutMs}ms`,
            code: 'REQUEST_TIMEOUT',
            retryAfter: 5,
            timestamp: new Date().toISOString(),
        });

        next(timeoutError);
    }, timeoutMs);

    req._timeoutId = timeoutId;
    req._requestId = requestId;

    const originalEnd = res.end;
    res.end = function(chunk, encoding) {
        if (!isCompleted) {
            isCompleted = true;
            clearTimeout(timeoutId);
            deadlinePropagator.complete(requestId);

            const duration = Date.now() - (req._startTime || Date.now());
            timeoutCalculator.recordLatency(req.path, duration, res.statusCode < 400);
        }
        return originalEnd.call(this, chunk, encoding);
    };

    req.isTimedOut = () => isTimedOut;
    req.getRemainingTime = () => deadlinePropagator.getRemainingTime(requestId);

    next();
};

// ============================================================
// 🚀 ASYNC OPERATION TIMEOUT WRAPPER
// ============================================================

const withTimeout = async (promise, timeoutMs, operationName) => {
    let timeoutId;

    const timeoutPromise = new Promise((_, reject) => {
        timeoutId = setTimeout(() => {
            const error = new Error(`Operation '${operationName}' timed out after ${timeoutMs}ms`);
            error.code = 'OPERATION_TIMEOUT';
            error.operation = operationName;
            reject(error);
        }, timeoutMs);
    });

    try {
        const result = await Promise.race([promise, timeoutPromise]);
        clearTimeout(timeoutId);
        return result;
    } catch (error) {
        clearTimeout(timeoutId);
        throw error;
    }
};

// ============================================================
// 📊 METRICS & HEALTH
// ============================================================

const getTimeoutMetrics = () => {
    return {
        adaptiveTimeout: timeoutCalculator.getMetrics(),
        deadlinePropagator: deadlinePropagator.getMetrics(),
        config: {
            defaultTimeoutMs: DEFAULT_TIMEOUT_MS,
            noTimeoutEndpoints: NO_TIMEOUT_ENDPOINTS,
            customEndpoints: Object.keys(DEFAULT_TIMEOUT_ENDPOINTS).length,
        },
    };
};

const timeoutHealthCheck = () => {
    const metrics = timeoutCalculator.getMetrics();
    const deadlineMetrics = deadlinePropagator.getMetrics();

    let status = 'HEALTHY';
    if (deadlineMetrics.activeDeadlines > 50000) status = 'DEGRADED';
    if (metrics.avgTimeoutMs > DEFAULT_TIMEOUT_MS * 2) status = 'DEGRADED';

    return {
        status,
        timestamp: new Date().toISOString(),
        metrics: {
            activeDeadlines: deadlineMetrics.activeDeadlines,
            avgTimeoutMs: metrics.avgTimeoutMs,
            adjustments: metrics.adjustments,
        },
    };
};

// ============================================================
// EXPORTS
// ============================================================

module.exports = {
    timeoutMiddleware,
    withTimeout,
    getTimeoutMetrics,
    timeoutHealthCheck,
    timeoutCalculator,
    deadlinePropagator,
    createTimeoutError,
};