const express = require('express');
const router = express.Router();
const orderController = require('../controllers/orderController');
const { protect, adminOnly } = require('../middleware/authMiddleware');
const crypto = require('crypto');
const axios = require('axios');
const Order = require('../models/orderModel');

// ----------------------------
// 🚀 ALGORITHM 1: IRT (Intelligent Routing & Throttling)
// ----------------------------

// ----------------------------
// 🧠 NEW ALGORITHM: DCR (Dynamic Circuit Routing)
// ----------------------------
console.log('🔥🔥🔥 orderRoutes.js is LOADING at:', new Date().toISOString());

// Add this test route BEFORE everything else
router.get('/test-route', (req, res) => {
    res.json({ message: 'Route file is loaded!', timestamp: Date.now() });
});

// DCR Service Health Registry
class DynamicCircuitRouter {
    constructor() {
        this.services = {
            'user-service': {
                state: 'CLOSED',
                healthScore: 1.0,
                consecutiveFailures: 0,
                lastFailureAt: null,
                lastSuccessAt: Date.now(),
                latencyP99: 0,
                errorRate: 0,
                circuitOpenedAt: null,
                circuitCooldownMs: 30000,
            },
            'product-service': {
                state: 'CLOSED',
                healthScore: 1.0,
                consecutiveFailures: 0,
                lastFailureAt: null,
                lastSuccessAt: Date.now(),
                latencyP99: 0,
                errorRate: 0,
                circuitOpenedAt: null,
                circuitCooldownMs: 30000,
            },
        };

        this.degradedCache = new Map();
        this.cacheTTL = 60 * 1000;
        this.requestWindow = [];
        this.windowSizeMs = 60000;

        this._startHealthChecks();
    }

    updateServiceHealth(serviceName, success, latencyMs, errorType = null) {
        const service = this.services[serviceName];
        if (!service) return;

        const now = Date.now();

        this.requestWindow.push({ service: serviceName, success, latencyMs, timestamp: now });
        this.requestWindow = this.requestWindow.filter(w => now - w.timestamp < this.windowSizeMs);

        const recentRequests = this.requestWindow.filter(w => w.service === serviceName);
        const totalRequests = recentRequests.length;
        const successfulRequests = recentRequests.filter(w => w.success).length;
        const latencies = recentRequests.map(w => w.latencyMs).sort((a,b) => a - b);

        service.successRate = totalRequests > 0 ? successfulRequests / totalRequests : 1;
        service.errorRate = totalRequests > 0 ? 1 - service.successRate : 0;
        service.latencyP99 = latencies[Math.floor(latencies.length * 0.99)] || 0;

        const latencyScore = Math.max(0, 1 - (service.latencyP99 / 500));
        const successScore = service.successRate;
        const errorPenalty = service.errorRate;

        service.healthScore = (successScore * 0.5) + (latencyScore * 0.3) + ((1 - errorPenalty) * 0.2);
        service.healthScore = Math.min(1.0, Math.max(0, service.healthScore));

        if (!success) {
            service.consecutiveFailures++;
            service.lastFailureAt = now;

            if (service.consecutiveFailures >= 5 || service.errorRate > 0.6) {
                if (service.state !== 'OPEN') {
                    service.state = 'OPEN';
                    service.circuitOpenedAt = now;
                    console.error(`[DCR] ⚡ Circuit OPEN for ${serviceName}`);
                }
            } else if (service.consecutiveFailures >= 3 && service.state === 'CLOSED') {
                service.state = 'DEGRADED';
                console.warn(`[DCR] ⚠️ Circuit DEGRADED for ${serviceName}`);
            }
        } else {
            service.consecutiveFailures = 0;
            service.lastSuccessAt = now;

            if (service.state === 'HALF_OPEN') {
                service.state = 'CLOSED';
                console.log(`[DCR] ✅ Circuit CLOSED for ${serviceName}`);
            } else if (service.state === 'DEGRADED' && service.healthScore > 0.7) {
                service.state = 'CLOSED';
                console.log(`[DCR] ✅ Circuit CLOSED for ${serviceName}`);
            }
        }

        if (service.state === 'OPEN' && service.circuitOpenedAt) {
            const timeInOpen = now - service.circuitOpenedAt;
            if (timeInOpen >= service.circuitCooldownMs) {
                service.state = 'HALF_OPEN';
                console.log(`[DCR] 🔄 Circuit HALF_OPEN for ${serviceName}`);
            }
        }

        return service;
    }

    isCircuitAllowed(serviceName) {
        const service = this.services[serviceName];
        if (!service) return true;

        switch (service.state) {
            case 'OPEN': return false;
            case 'HALF_OPEN': return Math.random() < 0.3;
            case 'DEGRADED': return Math.random() < 0.7;
            default: return true;
        }
    }

    getCachedData(serviceName, cacheKey) {
        const cacheEntry = this.degradedCache.get(`${serviceName}:${cacheKey}`);
        if (cacheEntry && Date.now() - cacheEntry.timestamp < this.cacheTTL) {
            return cacheEntry.data;
        }
        return null;
    }

    setCachedData(serviceName, cacheKey, data) {
        this.degradedCache.set(`${serviceName}:${cacheKey}`, {
            data,
            timestamp: Date.now(),
        });

        if (this.degradedCache.size > 1000) {
            for (const [key, value] of this.degradedCache.entries()) {
                if (Date.now() - value.timestamp > this.cacheTTL) {
                    this.degradedCache.delete(key);
                }
            }
        }
    }

    async executeWithCircuit(serviceName, operation, cacheKey = null, fallbackFn = null) {
        const startTime = Date.now();

        if (!this.isCircuitAllowed(serviceName)) {
            if (cacheKey) {
                const cachedData = this.getCachedData(serviceName, cacheKey);
                if (cachedData) {
                    return { data: cachedData, fromCache: true, circuitState: this.services[serviceName]?.state };
                }
            }

            if (fallbackFn) {
                try {
                    const fallbackData = await fallbackFn();
                    return { data: fallbackData, fromFallback: true, circuitState: this.services[serviceName]?.state };
                } catch (err) {
                    console.error(`[DCR] Fallback failed:`, err.message);
                }
            }

            throw new Error(`Circuit OPEN for ${serviceName}`);
        }

        try {
            const result = await operation();
            const latency = Date.now() - startTime;
            this.updateServiceHealth(serviceName, true, latency);

            if (cacheKey && result) {
                this.setCachedData(serviceName, cacheKey, result);
            }

            return { data: result, fromCache: false, circuitState: this.services[serviceName]?.state };
        } catch (error) {
            const latency = Date.now() - startTime;
            this.updateServiceHealth(serviceName, false, latency, error.message);

            if (cacheKey) {
                const cachedData = this.getCachedData(serviceName, cacheKey);
                if (cachedData) {
                    console.warn(`[DCR] ⚠️ Service ${serviceName} failed, using cached response`);
                    return { data: cachedData, fromCache: true, circuitState: this.services[serviceName]?.state };
                }
            }

            throw error;
        }
    }

    _startHealthChecks() {
        setInterval(() => {
            for (const [serviceName, service] of Object.entries(this.services)) {
                if (service.healthScore < 0.3 && service.state !== 'OPEN') {
                    console.error(`[DCR] 🚨 Critical health for ${serviceName}: score=${service.healthScore.toFixed(2)}`);
                }
            }
        }, 5000);
    }

    getMetrics() {
        const metrics = {};
        for (const [serviceName, service] of Object.entries(this.services)) {
            metrics[serviceName] = {
                state: service.state,
                healthScore: service.healthScore.toFixed(3),
                errorRate: (service.errorRate * 100).toFixed(1) + '%',
                latencyP99Ms: service.latencyP99,
                consecutiveFailures: service.consecutiveFailures,
            };
        }
        return metrics;
    }
}

// Initialize DCR
const dcr = new DynamicCircuitRouter();

// DCR Middleware
const dcrMiddleware = async (req, res, next) => {
    req.dcr = dcr;
    res.locals.dcrStartTime = Date.now();
    next();
};

router.use(dcrMiddleware);

// ----------------------------
// DCR Health Check Endpoint
// ----------------------------
router.get('/dcr/status', protect, async (req, res) => {
    const metrics = dcr.getMetrics();
    res.json({
        algorithm: 'DCR (Dynamic Circuit Routing)',
        timestamp: Date.now(),
        services: metrics,
        cacheSize: dcr.degradedCache.size,
    });
});

// ============================================================
// ⚠️ IMPORTANT: Specific routes MUST come BEFORE generic /:id route
// ============================================================

// ----------------------------
// 🚀 NEW: Webhook Routes (HERMES & SIREN Algorithms)
// ----------------------------
router.post('/webhooks/order-created', protect, async (req, res) => {
    if (orderController.webhookOrderCreated) {
        return orderController.webhookOrderCreated(req, res);
    }
    res.status(501).json({
        message: 'Order created webhook not implemented',
        algorithm: 'HERMES + SIREN'
    });
});

router.post('/webhooks/order-paid', protect, async (req, res) => {
    if (orderController.webhookOrderPaid) {
        return orderController.webhookOrderPaid(req, res);
    }
    res.status(501).json({
        message: 'Order paid webhook not implemented',
        algorithm: 'HERMES + SIREN'
    });
});

router.post('/webhooks/subscribe', protect, async (req, res) => {
    if (orderController.subscribeWebhook) {
        return orderController.subscribeWebhook(req, res);
    }
    res.status(501).json({ message: 'Webhook subscription not implemented' });
});

router.get('/webhooks/stats', protect, async (req, res) => {
    if (orderController.getWebhookStats) {
        return orderController.getWebhookStats(req, res);
    }
    res.status(501).json({ message: 'Webhook stats not implemented' });
});

router.get('/webhooks/dead-letter', protect, async (req, res) => {
    if (orderController.getDeadLetterQueue) {
        return orderController.getDeadLetterQueue(req, res);
    }
    res.status(501).json({ message: 'Dead letter queue not implemented' });
});

router.post('/webhooks/retry/:index', protect, async (req, res) => {
    if (orderController.retryWebhook) {
        return orderController.retryWebhook(req, res);
    }
    res.status(501).json({ message: 'Webhook retry not implemented' });
});

router.get('/webhooks/events', protect, async (req, res) => {
    if (orderController.getEventStream) {
        return orderController.getEventStream(req, res);
    }
    res.status(501).json({ message: 'Event stream not implemented' });
});

// ----------------------------
// 🚀 NEW: Search & Filter Routes (SPHINX & PRISM Algorithms)
// ----------------------------
router.get('/search', protect, async (req, res) => {
    if (orderController.searchOrders) {
        return orderController.searchOrders(req, res);
    }
    res.status(501).json({
        message: 'Search endpoint not implemented',
        algorithm: 'SPHINX (Search Parsing with Hierarchical Indexing for Nexus eXecution)'
    });
});

router.get('/filter', protect, async (req, res) => {
    if (orderController.filterOrders) {
        return orderController.filterOrders(req, res);
    }
    res.status(501).json({
        message: 'Filter endpoint not implemented',
        algorithm: 'PRISM (Progressive Range Indexing for Scalable Metrics)'
    });
});

router.get('/search/suggest', protect, async (req, res) => {
    if (orderController.getSearchSuggestions) {
        return orderController.getSearchSuggestions(req, res);
    }
    res.status(501).json({ message: 'Search suggestions not implemented' });
});

router.post('/search/cache/invalidate', protect, async (req, res) => {
    if (orderController.invalidateSearchCache) {
        return orderController.invalidateSearchCache(req, res);
    }
    res.status(501).json({ message: 'Search cache invalidation not implemented' });
});

// ----------------------------
// 🚀 NEW: Analytics Endpoints (TIDES & HELIX Algorithms)
// ----------------------------
router.get('/analytics/daily', protect, async (req, res) => {
    if (orderController.getDailyAnalytics) {
        return orderController.getDailyAnalytics(req, res);
    }
    res.status(501).json({
        message: 'Analytics endpoint not fully implemented',
        algorithm: 'TIDES (Temporal Intelligence for Dynamic Exponential Smoothing)'
    });
});

router.get('/analytics/top-products', protect, async (req, res) => {
    if (orderController.getTopProducts) {
        return orderController.getTopProducts(req, res);
    }
    res.status(501).json({
        message: 'Analytics endpoint not fully implemented',
        algorithm: 'HELIX (Hierarchical Exponential Learning for Intelligent eXecution)'
    });
});

router.post('/analytics/cache/invalidate', protect, async (req, res) => {
    if (orderController.invalidateAnalyticsCache) {
        return orderController.invalidateAnalyticsCache(req, res);
    }
    res.status(501).json({ message: 'Cache invalidation not implemented' });
});

// ----------------------------
// 🚀 NEW: Bulk Operations Routes (BATCH & STREAM Algorithms)
// ----------------------------
router.post('/bulk/cancel', protect, async (req, res) => {
    if (orderController.bulkCancelOrders) {
        return orderController.bulkCancelOrders(req, res);
    }
    res.status(501).json({ message: 'Bulk cancel not implemented' });
});

router.post('/bulk/export', protect, async (req, res) => {
    if (orderController.bulkExportOrders) {
        return orderController.bulkExportOrders(req, res);
    }
    res.status(501).json({ message: 'Bulk export not implemented' });
});

router.get('/bulk/export/status/:exportId', protect, async (req, res) => {
    if (orderController.getExportStatus) {
        return orderController.getExportStatus(req, res);
    }
    res.status(501).json({ message: 'Export status not implemented' });
});

router.get('/bulk/download/:exportId', protect, async (req, res) => {
    if (orderController.downloadExport) {
        return orderController.downloadExport(req, res);
    }
    res.status(501).json({ message: 'Download not implemented' });
});

router.post('/bulk/retry/:jobId', protect, async (req, res) => {
    if (orderController.retryFailedBatch) {
        return orderController.retryFailedBatch(req, res);
    }
    res.status(501).json({ message: 'Retry not implemented' });
});

router.get('/bulk/dead-letter', protect, async (req, res) => {
    if (orderController.getDeadLetterQueue) {
        return orderController.getDeadLetterQueue(req, res);
    }
    res.status(501).json({ message: 'Dead letter queue not implemented' });
});

// ----------------------------
// 🚀 NEW: Reorder & Tracking Routes (PHOENIX & TRACK Algorithms)
// ----------------------------
router.post('/bulk/reorder', protect, async (req, res) => {
    if (orderController.bulkReorder) {
        return orderController.bulkReorder(req, res);
    }
    res.status(501).json({ message: 'Bulk reorder not implemented' });
});

router.get('/reorder/stats', protect, async (req, res) => {
    if (orderController.getReorderStats) {
        return orderController.getReorderStats(req, res);
    }
    res.status(501).json({ message: 'Reorder stats not implemented' });
});

router.post('/tracking/webhook', protect, async (req, res) => {
    if (orderController.trackingWebhook) {
        return orderController.trackingWebhook(req, res);
    }
    res.status(501).json({ message: 'Tracking webhook not implemented' });
});

router.post('/:id/reorder', protect, async (req, res) => {
    if (orderController.reorderOrder) {
        return orderController.reorderOrder(req, res);
    }
    res.status(501).json({
        message: 'Reorder endpoint not implemented',
        algorithm: 'PHOENIX (Predictive Historical Order Execution with Neural Index eXecution)'
    });
});

router.get('/:id/tracking', protect, async (req, res) => {
    if (orderController.getOrderTracking) {
        return orderController.getOrderTracking(req, res);
    }
    res.status(501).json({
        message: 'Tracking endpoint not implemented',
        algorithm: 'TRACK (Tracking & Real-time Analytics with Carrier Knowledge)'
    });
});

// ----------------------------
// 🚀 NEW: Admin Dashboard Routes (PENDULUM & ABACUS Algorithms)
// ----------------------------
// PENDULUM algorithm for intelligent pending order aggregation
// ABACUS algorithm for abandoned cart detection with recovery scoring
// ----------------------------

/**
 * @route GET /api/admin/orders/pending
 * @desc Get all pending orders with PENDULUM algorithm
 * @access Private/Admin
 * @query page - Page number (default: 1)
 * @query limit - Items per page (default: 20, max: 100)
 */
router.get('/admin/orders/pending', protect, async (req, res) => {
    if (orderController.getPendingOrders) {
        return orderController.getPendingOrders(req, res);
    }
    res.status(501).json({
        message: 'Pending orders endpoint not implemented',
        algorithm: 'PENDULUM (Pending Order Discovery & Unified Listing Utility Module)'
    });
});

/**
 * @route GET /api/admin/orders/abandoned-carts
 * @desc Get abandoned carts with ABACUS algorithm
 * @access Private/Admin
 * @query minutes - Time threshold in minutes (default: 10)
 * @query page - Page number (default: 1)
 * @query limit - Items per page (default: 20, max: 100)
 */
router.get('/admin/orders/abandoned-carts', protect, async (req, res) => {
    if (orderController.getAbandonedCarts) {
        return orderController.getAbandonedCarts(req, res);
    }
    res.status(501).json({
        message: 'Abandoned carts endpoint not implemented',
        algorithm: 'ABACUS (Abandoned Basket Analytics & Cumulative Unified Scoring)'
    });
});

// ----------------------------
// ✅ EXISTING ROUTES (Generic routes - MUST come AFTER specific ones)
// ----------------------------
router.post('/', protect, orderController.createOrder);
router.post('/:id/confirm', protect, async (req, res) => {
    if (orderController.confirmOrder) {
        return orderController.confirmOrder(req, res);
    }

    try {
        const order = await Order.findById(req.params.id);
        if (!order) return res.status(404).json({ message: 'Order not found' });

        order.status = 'payment_received';
        order.twoPhaseState = 'COMPLETED';
        order.paidAt = new Date();
        await order.save();

        res.json({ message: 'Order confirmed', orderId: order._id });
    } catch (err) {
        res.status(500).json({ message: 'Server error' });
    }
});

router.get('/metrics/rio', protect, async (req, res) => {
    if (orderController.getRIOMetrics) {
        return orderController.getRIOMetrics(req, res);
    }

    const pendingOrders = await Order.countDocuments({ status: 'pending_payment' });
    res.json({
        algorithm: 'RIO',
        pendingPayments: pendingOrders,
        activeReservations: 0,
    });
});

router.post('/recover-abandoned', protect, async (req, res) => {
    try {
        const { userId } = req.body;
        const abandonedOrders = await Order.find({
            'user.userId': userId,
            status: 'pending_payment',
            createdAt: { $lt: new Date(Date.now() - 5 * 60 * 1000) },
        });

        res.json({
            message: 'Recovery initiated',
            abandonedCount: abandonedOrders.length,
            orders: abandonedOrders.map(o => ({ id: o._id, totalAmount: o.totalAmount })),
        });
    } catch (err) {
        res.status(500).json({ message: 'Server error' });
    }
});

router.post('/bulk/status', protect, async (req, res) => {
    try {
        const { orderIds, status } = req.body;
        const allowed = ['processing', 'shipped', 'delivered', 'cancelled'];

        if (!allowed.includes(status)) {
            return res.status(400).json({ message: 'Invalid status' });
        }

        const result = await Order.updateMany(
            { _id: { $in: orderIds } },
            { status, updatedAt: Date.now() }
        );

        res.json({ message: `Updated ${result.modifiedCount} orders`, modifiedCount: result.modifiedCount });
    } catch (err) {
        res.status(500).json({ message: 'Server error' });
    }
});

router.get('/health/dashboard', protect, async (req, res) => {
    const dcrMetrics = dcr.getMetrics();

    res.json({
        timestamp: Date.now(),
        orderService: { status: 'healthy', uptime: process.uptime() },
        dcr: dcrMetrics,
        overallHealth: 'GREEN',
    });
});

// Generic routes with parameters (must be LAST)
router.get('/user/:userId', protect, orderController.getOrdersByUser);
router.get('/:id', protect, orderController.getOrderById);
router.put('/:id/status', protect, orderController.updateOrderStatus);
router.delete('/:id', protect, orderController.cancelOrder);

module.exports = router;