const express = require('express');
const router = express.Router();
const productController = require('../controllers/productController');

// ----------------------------
// Algorithm 1: Adaptive Route Execution Pipeline (AREP)
// ----------------------------

const routeConfig = {
    // Core CRUD
    GET_ALL: { feature: 'FEATURE_PRODUCT_PAGINATION', priority: 10 },
    GET_ONE: { feature: 'FEATURE_PRODUCT_PAGINATION', priority: 9 },
    CREATE: { feature: 'FEATURE_CREATE_PRODUCT', priority: 10 },
    UPDATE: { feature: 'FEATURE_UPDATE_PRODUCT', priority: 8 },
    DELETE: { feature: 'FEATURE_DELETE_PRODUCT', priority: 5 },

    // Cache monitoring
    GET_CACHE_STATS: { feature: 'FEATURE_PRODUCT_PAGINATION', priority: 9 },

    // Reservation endpoints
    RESERVE_STOCK: { feature: 'FEATURE_CREATE_PRODUCT', priority: 8 },
    GET_RESERVATION: { feature: 'FEATURE_PRODUCT_PAGINATION', priority: 7 },
    RELEASE_RESERVATION: { feature: 'FEATURE_UPDATE_PRODUCT', priority: 6 },
    RESERVATION_METRICS: { feature: 'FEATURE_PRODUCT_PAGINATION', priority: 9 },
};

const adaptiveRoute = (configKey) => {
    return (req, res, next) => {
        const config = routeConfig[configKey];
        const feature = req.features?.[config.feature];

        if (!feature || !feature.enabled) {
            return res.status(503).json({
                message: `Feature ${config.feature} is currently disabled`,
            });
        }

        const systemLoad = process.memoryUsage().heapUsed / 1024 / 1024;
        if (systemLoad > 500 && config.priority < 8) {
            return res.status(503).json({
                message: 'System under heavy load, low priority route disabled',
            });
        }

        next();
    };
};

// ----------------------------
// Algorithm 2: Predictive Latency-Aware Circuit Breaker (PERMANENT FIX)
// ----------------------------

class PredictiveCircuitBreaker {
    constructor() {
        this.circuits = new Map();
        this.latencyHistory = new Map();
        this.predictionWindow = 60;
        this.startBackgroundTasks();
    }

    getCircuit(endpoint) {
        if (!this.circuits.has(endpoint)) {
            this.circuits.set(endpoint, {
                state: 'CLOSED',
                failures: 0,
                consecutiveFailures: 0,
                lastFailureTime: null,
                lastSuccessTime: Date.now(),
                consecutiveSuccesses: 0,
                p50Latency: 0,
                p95Latency: 0,
                p99Latency: 0,
                predictedFailureRate: 0,
                lastOpenedAt: null,
                halfOpenStartTime: null,
                requestCount: 0
            });
        }
        return this.circuits.get(endpoint);
    }

    recordRequest(endpoint, latencyMs, success) {
        const circuit = this.getCircuit(endpoint);
        circuit.requestCount++;

        if (!this.latencyHistory.has(endpoint)) {
            this.latencyHistory.set(endpoint, []);
        }

        const history = this.latencyHistory.get(endpoint);
        history.push({ latency: latencyMs, timestamp: Date.now(), success });

        const cutoff = Date.now() - (this.predictionWindow * 1000);
        while (history.length > 0 && history[0].timestamp < cutoff) {
            history.shift();
        }

        if (success) {
            circuit.consecutiveSuccesses++;
            circuit.consecutiveFailures = 0;
            circuit.lastSuccessTime = Date.now();

            if (circuit.state === 'HALF_OPEN') {
                if (circuit.consecutiveSuccesses >= 2) {
                    circuit.state = 'CLOSED';
                    circuit.failures = 0;
                    circuit.consecutiveSuccesses = 0;
                    circuit.halfOpenStartTime = null;
                    console.log(`✅ Circuit CLOSED for ${endpoint} - recovered`);
                }
            } else if (circuit.state === 'CLOSED') {
                circuit.failures = Math.max(0, circuit.failures - 0.3);
            }
        } else {
            circuit.consecutiveFailures++;
            circuit.consecutiveSuccesses = 0;
            circuit.lastFailureTime = Date.now();

            if (circuit.state === 'HALF_OPEN') {
                circuit.state = 'OPEN';
                circuit.lastOpenedAt = Date.now();
                circuit.halfOpenStartTime = null;
                circuit.consecutiveFailures = 0;
                console.error(`🔌 Circuit re-OPENED for ${endpoint} - HALF_OPEN test failed`);
            }

            // Only open circuit after at least 5 requests
            const failureRate = circuit.failures / Math.max(1, circuit.requestCount);

            if (circuit.state === 'CLOSED' && circuit.requestCount >= 5 &&
                (circuit.consecutiveFailures >= 3 || circuit.failures >= 5 || failureRate > 0.6)) {
                circuit.state = 'OPEN';
                circuit.lastOpenedAt = Date.now();
                circuit.failures = 0;
                console.error(`🔌 Circuit OPEN for ${endpoint} - consecutive failures: ${circuit.consecutiveFailures}, rate: ${(failureRate * 100).toFixed(1)}%`);
            }

            circuit.failures++;
        }

        this.updateLatencyPercentiles(endpoint);
        this.updatePredictedFailureRate(endpoint);
    }

    updateLatencyPercentiles(endpoint) {
        const history = this.latencyHistory.get(endpoint);
        if (!history || history.length < 10) return;

        const latencies = history.map(h => h.latency).sort((a, b) => a - b);
        const circuit = this.circuits.get(endpoint);

        circuit.p50Latency = latencies[Math.floor(latencies.length * 0.5)];
        circuit.p95Latency = latencies[Math.floor(latencies.length * 0.95)];
        circuit.p99Latency = latencies[Math.floor(latencies.length * 0.99)];
    }

    updatePredictedFailureRate(endpoint) {
        const history = this.latencyHistory.get(endpoint);
        if (!history || history.length < 10) return;

        const recentFailures = history.slice(-20).filter(h => !h.success).length;
        const recentRate = recentFailures / Math.min(20, history.length);
        const circuit = this.circuits.get(endpoint);
        const alpha = 0.3;

        circuit.predictedFailureRate = (alpha * recentRate) + ((1 - alpha) * (circuit.predictedFailureRate || 0));
    }

    shouldAllowRequest(endpoint) {
        const circuit = this.getCircuit(endpoint);
        const now = Date.now();

        // Allow new endpoints to establish baseline
        if (circuit.requestCount < 5) {
            return true;
        }

        if (circuit.state === 'CLOSED') {
            if (circuit.predictedFailureRate > 0.7) {
                console.warn(`⚠️ Predictive rejection for ${endpoint} - rate: ${(circuit.predictedFailureRate * 100).toFixed(1)}%`);
                return false;
            }
            return true;
        }

        if (circuit.state === 'OPEN') {
            const openDuration = (now - circuit.lastOpenedAt) / 1000;
            const recoveryTimeout = this.calculateRecoveryTimeout(circuit);

            if (openDuration >= recoveryTimeout) {
                circuit.state = 'HALF_OPEN';
                circuit.consecutiveSuccesses = 0;
                circuit.halfOpenStartTime = now;
                console.log(`🔄 Circuit HALF_OPEN for ${endpoint} - testing recovery`);
                return true;
            }
            return false;
        }

        if (circuit.state === 'HALF_OPEN') {
            if (circuit.halfOpenStartTime && (now - circuit.halfOpenStartTime) > 30000) {
                circuit.state = 'CLOSED';
                circuit.failures = 0;
                circuit.consecutiveSuccesses = 0;
                circuit.halfOpenStartTime = null;
                console.log(`🔄 Force closed HALF_OPEN circuit for ${endpoint} - timeout`);
                return true;
            }
            return true;
        }

        return true;
    }

    calculateRecoveryTimeout(circuit) {
        const baseTimeout = 15000;
        const severityMultiplier = Math.min(2, 1 + (circuit.predictedFailureRate || 0));
        const jitter = Math.random() * 3000;
        return baseTimeout * severityMultiplier + jitter;
    }

    getRetryAfter(endpoint) {
        const circuit = this.circuits.get(endpoint);
        if (!circuit || circuit.state !== 'OPEN') return 2;

        const openDuration = (Date.now() - circuit.lastOpenedAt) / 1000;
        const recoveryTimeout = this.calculateRecoveryTimeout(circuit);
        return Math.max(1, Math.ceil((recoveryTimeout - openDuration) / 1000));
    }

    getStatus(endpoint) {
        const circuit = this.circuits.get(endpoint);
        if (!circuit) return { state: 'CLOSED', healthy: true };

        return {
            state: circuit.state,
            healthy: circuit.state === 'CLOSED' && circuit.predictedFailureRate < 0.5,
            p50Latency: circuit.p50Latency,
            p95Latency: circuit.p95Latency,
            p99Latency: circuit.p99Latency,
            predictedFailureRate: (circuit.predictedFailureRate * 100).toFixed(1),
            failures: circuit.failures,
            requestCount: circuit.requestCount
        };
    }

    startBackgroundTasks() {
        setInterval(() => {
            for (const [endpoint, circuit] of this.circuits) {
                if (circuit.state !== 'CLOSED') {
                    console.warn(`📊 Circuit [${endpoint}]: ${circuit.state}`);
                }

                if (circuit.state === 'HALF_OPEN' && (Date.now() - circuit.lastSuccessTime) > 45000) {
                    circuit.state = 'CLOSED';
                    circuit.halfOpenStartTime = null;
                    circuit.consecutiveSuccesses = 0;
                    console.log(`🔄 Auto-closed stale HALF_OPEN circuit for ${endpoint}`);
                }
            }
        }, 30000);
    }
}

const circuitBreaker = new PredictiveCircuitBreaker();

const circuitBreakerMiddleware = (endpoint) => {
    return async (req, res, next) => {
        const startTime = Date.now();

        if (!circuitBreaker.shouldAllowRequest(endpoint)) {
            const retryAfter = circuitBreaker.getRetryAfter(endpoint);
            return res.status(503).json({
                error: 'Circuit breaker is OPEN',
                message: 'Service temporarily unavailable due to high failure rate',
                retryAfter: retryAfter,
                endpoint: endpoint
            });
        }

        const originalJson = res.json;
        let success = true;

        res.json = function(data) {
            originalJson.call(this, data);
        };

        try {
            await next();
            success = res.statusCode < 400;
        } catch (error) {
            success = false;
            throw error;
        } finally {
            const latency = Date.now() - startTime;
            circuitBreaker.recordRequest(endpoint, latency, success);
        }
    };
};

// ----------------------------
// Product Routes
// ----------------------------

// GET products cache stats
router.get(
    '/cache/stats',
    adaptiveRoute('GET_CACHE_STATS'),
    circuitBreakerMiddleware('GET_CACHE_STATS'),
    productController.getCacheStats
);

// GET all products
router.get(
    '/',
    adaptiveRoute('GET_ALL'),
    circuitBreakerMiddleware('GET_ALL_PRODUCTS'),
    productController.getAllProducts
);

// GET single product
router.get(
    '/:id',
    adaptiveRoute('GET_ONE'),
    circuitBreakerMiddleware('GET_PRODUCT_BY_ID'),
    productController.getProductById
);

// CREATE product
router.post(
    '/',
    adaptiveRoute('CREATE'),
    circuitBreakerMiddleware('CREATE_PRODUCT'),
    productController.createProduct
);

// UPDATE product
router.put(
    '/:id',
    adaptiveRoute('UPDATE'),
    circuitBreakerMiddleware('UPDATE_PRODUCT'),
    productController.updateProduct
);

// DELETE product
router.delete(
    '/:id',
    adaptiveRoute('DELETE'),
    circuitBreakerMiddleware('DELETE_PRODUCT'),
    productController.deleteProduct
);

// ============================================
// Algorithm 3: Stock Reservation Routes
// ============================================

router.post(
    '/:id/reserve-stock',
    adaptiveRoute('RESERVE_STOCK'),
    circuitBreakerMiddleware('RESERVE_STOCK'),
    productController.reserveStock
);

router.get(
    '/reservation/:reservationId',
    adaptiveRoute('GET_RESERVATION'),
    circuitBreakerMiddleware('GET_RESERVATION'),
    productController.getReservationStatus
);

router.delete(
    '/reservation/:reservationId',
    adaptiveRoute('RELEASE_RESERVATION'),
    circuitBreakerMiddleware('RELEASE_RESERVATION'),
    productController.releaseReservation
);

router.get(
    '/reservation/metrics',
    adaptiveRoute('RESERVATION_METRICS'),
    circuitBreakerMiddleware('RESERVATION_METRICS'),
    productController.getReservationMetrics
);

// ============================================
// Monitoring Endpoints
// ============================================

router.get('/admin/circuit/status', (req, res) => {
    const endpoints = [
        'GET_ALL_PRODUCTS',
        'GET_PRODUCT_BY_ID',
        'CREATE_PRODUCT',
        'UPDATE_PRODUCT',
        'DELETE_PRODUCT',
        'RESERVE_STOCK',
        'GET_RESERVATION',
        'RELEASE_RESERVATION',
        'RESERVATION_METRICS'
    ];

    const status = {};
    endpoints.forEach(endpoint => {
        status[endpoint] = circuitBreaker.getStatus(endpoint);
    });

    res.json({
        algorithm: 'Predictive Latency-Aware Circuit Breaker + DIRE',
        status: status,
        features: {
            latencyPercentiles: 'p50, p95, p99 tracking',
            failurePrediction: 'EWMA-based with 60s window',
            autoRecovery: 'Jittered backoff with half-open state',
            predictiveRejection: 'Enabled when failure rate >70%',
            inventoryReservation: 'DIRE Algorithm Active'
        }
    });
});

router.get('/admin/circuit/:endpoint', (req, res) => {
    const { endpoint } = req.params;
    const status = circuitBreaker.getStatus(endpoint);

    res.json({
        endpoint: endpoint,
        ...status,
        timestamp: new Date().toISOString()
    });
});

module.exports = router;