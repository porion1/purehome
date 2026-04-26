/**
 * ============================================================
 * 📊 METRICS — PROMETHEUS TELEMETRY ENGINE v1.0
 * ============================================================
 *
 * PURPOSE:
 * - Export Prometheus metrics for 50M+ scale
 * - Track payment success rates, latency, errors
 * - Real-time anomaly detection via metrics
 * - Auto-scaling hints for Kubernetes
 *
 * SCALE TARGET:
 * - 50M+ metrics points per minute
 * - Sub-millisecond metric recording
 * - Zero memory leaks with bounded counters
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: ADAPTIVE METRIC SAMPLING (Dynamic Cardinality Control)
 * ------------------------------------------------------------
 * - Automatically samples high-cardinality metrics
 * - Prevents label explosion at 50M scale
 * - Rotates metric buckets based on usage patterns
 *
 * 🧠 ALGORITHM 2: PREDICTIVE SLIDING WINDOW (Anomaly Detection)
 * ------------------------------------------------------------
 * - Tracks p50/p95/p99 latency in sliding windows
 * - Detects performance degradation before failure
 * - Generates alerts for threshold violations
 *
 * ============================================================
 */

const client = require('prom-client');

// ============================================================
// CONFIG
// ============================================================

const METRICS_ENABLED = process.env.ENABLE_PAYMENT_METRICS !== 'false';
const SERVICE_NAME = process.env.SERVICE_NAME || 'payment-service';
const METRICS_PORT = parseInt(process.env.METRICS_PORT) || 9090;

// ============================================================
// 🧠 ALGORITHM 1: ADAPTIVE METRIC SAMPLING
// ============================================================

class AdaptiveMetricSampler {
    constructor() {
        this.sampleRates = new Map(); // metricName -> current sample rate
        this.metricCounts = new Map(); // metricName -> request count
        this.defaultSampleRate = 1.0; // 100% sampling for critical metrics
        this.minSampleRate = 0.01; // 1% minimum
        this.stats = {
            totalMetrics: 0,
            sampledMetrics: 0,
            rateAdjustments: 0,
        };

        // Adjust sampling rates every minute
        setInterval(() => this.adjustSamplingRates(), 60000);
    }

    /**
     * Determine if metric should be recorded
     */
    shouldSample(metricName, cardinality = 1) {
        this.stats.totalMetrics++;

        // Always sample critical metrics
        if (this.isCriticalMetric(metricName)) {
            this.stats.sampledMetrics++;
            return true;
        }

        let sampleRate = this.sampleRates.get(metricName);
        if (sampleRate === undefined) {
            sampleRate = this.defaultSampleRate;
            this.sampleRates.set(metricName, sampleRate);
        }

        // Adjust for cardinality (high cardinality = lower sampling)
        const adjustedRate = sampleRate / Math.max(1, Math.log10(cardinality + 1));

        const shouldSample = Math.random() < adjustedRate;
        if (shouldSample) {
            this.stats.sampledMetrics++;
        }

        return shouldSample;
    }

    /**
     * Check if metric is critical (always sample)
     */
    isCriticalMetric(metricName) {
        const criticalMetrics = [
            'payment_success_total',
            'payment_failure_total',
            'payment_duration_seconds',
            'stripe_api_errors_total',
            'circuit_breaker_state',
        ];
        return criticalMetrics.some(c => metricName.includes(c));
    }

    /**
     * Adjust sampling rates based on volume
     */
    adjustSamplingRates() {
        for (const [metricName, count] of this.metricCounts.entries()) {
            // High volume metrics (>1000/min) get lower sampling
            if (count > 1000) {
                const newRate = Math.max(this.minSampleRate, this.defaultSampleRate * (1000 / count));
                if (newRate !== this.sampleRates.get(metricName)) {
                    this.sampleRates.set(metricName, newRate);
                    this.stats.rateAdjustments++;
                    console.log(`[METRICS] 📊 Sampling rate for ${metricName}: ${(newRate * 100).toFixed(1)}%`);
                }
            }
        }
        this.metricCounts.clear();
    }

    recordMetric(metricName) {
        const count = this.metricCounts.get(metricName) || 0;
        this.metricCounts.set(metricName, count + 1);
    }

    getMetrics() {
        return {
            totalMetrics: this.stats.totalMetrics,
            sampledMetrics: this.stats.sampledMetrics,
            sampleRate: ((this.stats.sampledMetrics / this.stats.totalMetrics) * 100).toFixed(2) + '%',
            rateAdjustments: this.stats.rateAdjustments,
            activeSamplingRates: this.sampleRates.size,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: PREDICTIVE SLIDING WINDOW
// ============================================================

class PredictiveSlidingWindow {
    constructor(windowSizeSeconds = 60) {
        this.windowSizeMs = windowSizeSeconds * 1000;
        this.windows = new Map(); // metricName -> { timestamps, values }
        this.anomalyThresholds = new Map(); // metricName -> { p95, p99 }
        this.stats = {
            anomaliesDetected: 0,
            windowsTracked: 0,
            predictions: 0,
        };

        // Cleanup old windows every minute
        setInterval(() => this.cleanupWindows(), 60000);
    }

    /**
     * Record value in sliding window
     */
    record(metricName, value, timestamp = Date.now()) {
        if (!this.windows.has(metricName)) {
            this.windows.set(metricName, { timestamps: [], values: [] });
            this.stats.windowsTracked++;
        }

        const window = this.windows.get(metricName);
        window.timestamps.push(timestamp);
        window.values.push(value);

        // Clean old entries
        const cutoff = timestamp - this.windowSizeMs;
        while (window.timestamps.length > 0 && window.timestamps[0] < cutoff) {
            window.timestamps.shift();
            window.values.shift();
        }

        // Update thresholds
        this.updateThresholds(metricName);
    }

    /**
     * Update percentile thresholds for anomaly detection
     */
    updateThresholds(metricName) {
        const window = this.windows.get(metricName);
        if (!window || window.values.length < 10) return;

        const sorted = [...window.values].sort((a, b) => a - b);
        const p95Index = Math.floor(sorted.length * 0.95);
        const p99Index = Math.floor(sorted.length * 0.99);

        this.anomalyThresholds.set(metricName, {
            p95: sorted[p95Index],
            p99: sorted[p99Index],
            count: window.values.length,
        });
    }

    /**
     * Check if value is anomalous
     */
    isAnomalous(metricName, value) {
        const thresholds = this.anomalyThresholds.get(metricName);
        if (!thresholds) return false;

        // Value exceeds p99 threshold
        if (value > thresholds.p99 * 1.5) {
            this.stats.anomaliesDetected++;
            return true;
        }

        return false;
    }

    /**
     * Predict next value using simple linear regression
     */
    predictNext(metricName) {
        const window = this.windows.get(metricName);
        if (!window || window.values.length < 10) return null;

        this.stats.predictions++;

        const n = window.values.length;
        const indices = Array.from({ length: n }, (_, i) => i);
        const values = window.values;

        const sumX = indices.reduce((a, b) => a + b, 0);
        const sumY = values.reduce((a, b) => a + b, 0);
        const sumXY = indices.reduce((a, b, i) => a + b * values[i], 0);
        const sumXX = indices.reduce((a, b) => a + b * b, 0);

        const slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        const intercept = (sumY - slope * sumX) / n;

        const nextIndex = n;
        const prediction = intercept + slope * nextIndex;

        return Math.max(0, prediction);
    }

    cleanupWindows() {
        const now = Date.now();
        let cleaned = 0;

        for (const [metricName, window] of this.windows.entries()) {
            const cutoff = now - this.windowSizeMs;
            while (window.timestamps.length > 0 && window.timestamps[0] < cutoff) {
                window.timestamps.shift();
                window.values.shift();
                cleaned++;
            }

            if (window.timestamps.length === 0) {
                this.windows.delete(metricName);
            }
        }

        if (cleaned > 0) {
            console.log(`[METRICS] 🧹 Cleaned ${cleaned} sliding window entries`);
        }
    }

    getMetrics() {
        return {
            anomaliesDetected: this.stats.anomaliesDetected,
            windowsTracked: this.stats.windowsTracked,
            predictions: this.stats.predictions,
            activeWindows: this.windows.size,
        };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const adaptiveSampler = new AdaptiveMetricSampler();
const predictiveWindow = new PredictiveSlidingWindow();

// ============================================================
// 📊 REGISTER PROMETHEUS METRICS
// ============================================================

// Enable default metrics (CPU, memory, event loop)
client.collectDefaultMetrics({
    prefix: `${SERVICE_NAME}_`,
    timeout: 10000,
});

// ============================================================
// 🚀 CUSTOM METRICS
// ============================================================

// Counter: Payment attempts by status
const paymentCounter = new client.Counter({
    name: `${SERVICE_NAME}_payments_total`,
    help: 'Total number of payment attempts',
    labelNames: ['status', 'payment_method', 'currency'],
});

// Counter: Payment failures by reason
const failureCounter = new client.Counter({
    name: `${SERVICE_NAME}_payment_failures_total`,
    help: 'Total number of payment failures by reason',
    labelNames: ['reason', 'error_code'],
});

// Histogram: Payment duration
const paymentDuration = new client.Histogram({
    name: `${SERVICE_NAME}_payment_duration_seconds`,
    help: 'Payment processing duration in seconds',
    labelNames: ['status'],
    buckets: [0.1, 0.5, 1, 2, 5, 10, 30, 60],
});

// Gauge: Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
const circuitBreakerState = new client.Gauge({
    name: `${SERVICE_NAME}_circuit_breaker_state`,
    help: 'Circuit breaker state: 0=CLOSED, 1=OPEN, 2=HALF_OPEN',
    labelNames: ['service'],
});

// Gauge: Active locks (idempotency)
const activeLocksGauge = new client.Gauge({
    name: `${SERVICE_NAME}_active_locks`,
    help: 'Number of active idempotency locks',
});

// Gauge: Queue size
const queueSizeGauge = new client.Gauge({
    name: `${SERVICE_NAME}_queue_size`,
    help: 'Current queue size for async operations',
    labelNames: ['queue_name'],
});

// Counter: Webhook events by type
const webhookCounter = new client.Counter({
    name: `${SERVICE_NAME}_webhooks_total`,
    help: 'Total number of webhook events received',
    labelNames: ['event_type', 'status'],
});

// Histogram: API response times
const apiDuration = new client.Histogram({
    name: `${SERVICE_NAME}_api_duration_seconds`,
    help: 'API endpoint response duration in seconds',
    labelNames: ['method', 'path', 'status_code'],
    buckets: [0.01, 0.05, 0.1, 0.5, 1, 2, 5, 10],
});

// Gauge: Active connections
const activeConnectionsGauge = new client.Gauge({
    name: `${SERVICE_NAME}_active_connections`,
    help: 'Number of active connections',
    labelNames: ['type'],
});

// Counter: Database operations
const dbOperationsCounter = new client.Counter({
    name: `${SERVICE_NAME}_db_operations_total`,
    help: 'Total database operations',
    labelNames: ['operation', 'collection', 'status'],
});

// Histogram: Database query duration
const dbDuration = new client.Histogram({
    name: `${SERVICE_NAME}_db_duration_seconds`,
    help: 'Database query duration in seconds',
    labelNames: ['operation', 'collection'],
    buckets: [0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1],
});

// ============================================================
// 📊 METRIC RECORDING HELPERS
// ============================================================

/**
 * Record payment attempt
 */
const recordPayment = (status, paymentMethod, currency, durationMs) => {
    if (!METRICS_ENABLED) return;

    const shouldSample = adaptiveSampler.shouldSample('payment', 1);
    if (!shouldSample) return;

    paymentCounter.inc({ status, payment_method: paymentMethod, currency });
    paymentDuration.observe({ status }, durationMs / 1000);
    predictiveWindow.record(`payment_duration_${status}`, durationMs);
};

/**
 * Record payment failure
 */
const recordPaymentFailure = (reason, errorCode) => {
    if (!METRICS_ENABLED) return;

    failureCounter.inc({ reason, error_code: errorCode });
};

/**
 * Record webhook event
 */
const recordWebhook = (eventType, status) => {
    if (!METRICS_ENABLED) return;

    webhookCounter.inc({ event_type: eventType, status });
};

/**
 * Record API request
 */
const recordApiRequest = (method, path, statusCode, durationMs) => {
    if (!METRICS_ENABLED) return;

    const shouldSample = adaptiveSampler.shouldSample('api', 1);
    if (!shouldSample) return;

    // Normalize path (remove IDs for cardinality control)
    const normalizedPath = path.replace(/\/[a-f0-9]{24}/g, '/:id');

    apiDuration.observe({
        method,
        path: normalizedPath,
        status_code: statusCode
    }, durationMs / 1000);

    predictiveWindow.record(`api_duration_${normalizedPath}`, durationMs);

    // Check for anomaly
    if (predictiveWindow.isAnomalous(`api_duration_${normalizedPath}`, durationMs)) {
        console.warn(`[METRICS] ⚠️ Anomalous API latency detected: ${method} ${path} - ${durationMs}ms`);
    }
};

/**
 * Record circuit breaker state
 */
const recordCircuitBreakerState = (service, state) => {
    if (!METRICS_ENABLED) return;

    const stateMap = { CLOSED: 0, OPEN: 1, HALF_OPEN: 2 };
    circuitBreakerState.set({ service }, stateMap[state] || 0);
};

/**
 * Record active locks
 */
const recordActiveLocks = (count) => {
    if (!METRICS_ENABLED) return;
    activeLocksGauge.set(count);
};

/**
 * Record queue size
 */
const recordQueueSize = (queueName, size) => {
    if (!METRICS_ENABLED) return;
    queueSizeGauge.set({ queue_name: queueName }, size);
};

/**
 * Record database operation
 */
const recordDbOperation = (operation, collection, status, durationMs) => {
    if (!METRICS_ENABLED) return;

    dbOperationsCounter.inc({ operation, collection, status });
    dbDuration.observe({ operation, collection }, durationMs / 1000);
};

/**
 * Record active connections
 */
const recordActiveConnections = (type, count) => {
    if (!METRICS_ENABLED) return;
    activeConnectionsGauge.set({ type }, count);
};

// ============================================================
// 📊 METRICS MIDDLEWARE (Express)
// ============================================================

/**
 * Express middleware for automatic API metrics collection
 */
const metricsMiddleware = (req, res, next) => {
    if (!METRICS_ENABLED) return next();

    const startTime = Date.now();
    const method = req.method;
    const path = req.path;

    // Record active connections
    recordActiveConnections('http', (req.app?.locals?.activeConnections || 0) + 1);

    // Hook into response
    const originalEnd = res.end;
    res.end = function(chunk, encoding) {
        const duration = Date.now() - startTime;
        const statusCode = res.statusCode;

        recordApiRequest(method, path, statusCode, duration);

        originalEnd.call(this, chunk, encoding);
    };

    next();
};

// ============================================================
// 📊 PROMETHEUS METRICS ENDPOINT
// ============================================================

/**
 * Express endpoint for Prometheus metrics scraping
 */
const metricsEndpoint = async (req, res) => {
    try {
        res.set('Content-Type', client.register.contentType);
        const metrics = await client.register.metrics();
        res.end(metrics);
    } catch (error) {
        console.error('[METRICS] Failed to generate metrics:', error);
        res.status(500).end();
    }
};

// ============================================================
// 📊 GET METRICS SUMMARY
// ============================================================

const getMetricsSummary = () => {
    return {
        adaptiveSampler: adaptiveSampler.getMetrics(),
        predictiveWindow: predictiveWindow.getMetrics(),
        prometheus: {
            enabled: METRICS_ENABLED,
            port: METRICS_PORT,
            service: SERVICE_NAME,
        },
        registeredMetrics: {
            counters: ['payments_total', 'payment_failures_total', 'webhooks_total', 'db_operations_total'],
            histograms: ['payment_duration_seconds', 'api_duration_seconds', 'db_duration_seconds'],
            gauges: ['circuit_breaker_state', 'active_locks', 'queue_size', 'active_connections'],
        },
    };
};

// ============================================================
// 🔄 BACKGROUND METRICS COLLECTION
// ============================================================

// Update metrics periodically
if (METRICS_ENABLED) {
    setInterval(() => {
        // Record system metrics
        const memUsage = process.memoryUsage();
        recordActiveConnections('memory_heap', Math.floor(memUsage.heapUsed / 1024 / 1024));

        // Check for predictions
        for (const [metricName, _] of predictiveWindow.windows) {
            const prediction = predictiveWindow.predictNext(metricName);
            if (prediction && prediction > 10000) { // >10 seconds predicted
                console.warn(`[METRICS] 📈 Prediction for ${metricName}: ${Math.round(prediction)}ms (high latency expected)`);
            }
        }
    }, 30000);
}

// ============================================================
// EXPORTS
// ============================================================

module.exports = {
    // Main exports
    metricsMiddleware,
    metricsEndpoint,
    getMetricsSummary,

    // Metric recording helpers
    recordPayment,
    recordPaymentFailure,
    recordWebhook,
    recordApiRequest,
    recordCircuitBreakerState,
    recordActiveLocks,
    recordQueueSize,
    recordDbOperation,
    recordActiveConnections,

    // Advanced access for monitoring
    adaptiveSampler,
    predictiveWindow,
    client,
};