const express = require('express');
const router = express.Router();
const crypto = require('crypto');

const paymentController = require('../controllers/paymentController');
const refundController = require('../controllers/refundController');

// Import DB Helper
const { getDBMetrics, healthCheck: dbHealthCheck } = require('../utils/dbHelper');

// ============================================================
// SIMPLE TEST ROUTES (KEEP THESE WORKING)
// ============================================================

router.get('/test1', (req, res) => {
    res.json({ success: true, message: 'Test route 1 works!' });
});

router.post('/test2', (req, res) => {
    res.json({ success: true, message: 'Test route 2 works!', received: req.body });
});

router.post('/test3', (req, res) => {
    const idempotencyKey = req.headers['idempotency-key'];
    if (!idempotencyKey) {
        return res.status(400).json({ success: false, message: 'Missing Idempotency-Key header' });
    }
    res.json({ success: true, message: 'Test route 3 works!', idempotencyKey });
});

router.get('/health', (req, res) => {
    res.json({ success: true, message: 'Payment routes are working!' });
});

// ============================================================
// IDEMPOTENCY GUARD
// ============================================================
const idempotencyCache = new Map();
const IDEMPOTENCY_TTL = 5 * 60 * 1000;

const edgeIdempotencyGuard = (req, res, next) => {
    const key = req.headers['idempotency-key'];
    if (!key) {
        return res.status(400).json({ error: 'Missing Idempotency-Key header' });
    }
    const existing = idempotencyCache.get(key);
    if (existing && existing.status === 'processing') {
        return res.status(409).json({ error: 'Duplicate request in progress' });
    }
    if (existing && existing.status === 'completed') {
        return res.status(200).json(existing.response);
    }
    idempotencyCache.set(key, { status: 'processing', createdAt: Date.now() });
    const originalJson = res.json.bind(res);
    res.json = (body) => {
        idempotencyCache.set(key, { status: 'completed', response: body, createdAt: Date.now() });
        return originalJson(body);
    };
    next();
};

// ============================================================
// REAL PAYMENT ROUTE - MINIMAL MIDDLEWARE (NO THROTTLE)
// ============================================================

router.post('/create-intent',
    edgeIdempotencyGuard,
    paymentController.createPaymentIntent
);

router.post('/confirm',
    edgeIdempotencyGuard,
    paymentController.confirmPayment
);

router.post('/refund',
    edgeIdempotencyGuard,
    (req, res) => refundController.processRefund(req, res)
);

// ============================================================
// ADD THIS CANCEL ROUTE
// ============================================================
router.post('/cancel',
    edgeIdempotencyGuard,
    paymentController.cancelPayment
);
// ============================================================

router.get('/:orderId',
    paymentController.getPaymentStatus
);

// ============================================================
// DB METRICS
// ============================================================
router.get('/db-metrics', async (req, res) => {
    try {
        const metrics = getDBMetrics();
        const health = dbHealthCheck();
        res.json({ success: true, database: metrics, health });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

console.log('[ROUTES] ✅ Routes registered:');
console.log('[ROUTES]   GET  /test1, /test2, /test3, /health');
console.log('[ROUTES]   POST /create-intent, /confirm, /refund');
console.log('[ROUTES]   GET  /:paymentId, /db-metrics');

module.exports = router;