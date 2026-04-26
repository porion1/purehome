/**
 * ============================================================
 * ⚡ APP.JS — EXPRESS APPLICATION CONFIGURATION v3.0 (TESTING MODE)
 * ============================================================
 * WITH JWT AUTH FOR PROPER USER ID EXTRACTION
 * ============================================================
 */

const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const jwt = require('jsonwebtoken');

// Load environment variables
dotenv.config();

// ============================================================
// 📦 IMPORT DB HELPER FOR DATABASE HEALTH MONITORING
// ============================================================
const { healthCheck: dbHealthCheck, getDBMetrics } = require('./utils/dbHelper');

// ============================================================
// CREATE EXPRESS APP
// ============================================================

const app = express();

// ============================================================
// 🚀 HEALTH CHECK ENDPOINTS - MUST BE FIRST (No middleware)
// ============================================================

app.get('/health/live', (req, res) => {
    res.status(200).json({
        status: 'alive',
        timestamp: new Date().toISOString(),
        uptime: process.uptime()
    });
});

app.get('/health/ready', (req, res) => {
    res.status(200).json({
        status: 'ready',
        timestamp: new Date().toISOString(),
        uptime: process.uptime()
    });
});

app.get('/health', (req, res) => {
    const dbHealth = dbHealthCheck();
    res.status(200).json({
        status: 'ok',
        service: 'payment-service',
        timestamp: new Date().toISOString(),
        uptime: process.uptime(),
        memory: process.memoryUsage().rss / 1024 / 1024,
        database: {
            status: dbHealth.status,
            ready: dbHealth.ready,
            circuitState: dbHealth.circuitState,
            healthScore: dbHealth.healthScore
        }
    });
});

app.get('/ping', (req, res) => {
    res.status(200).json({
        pong: true,
        timestamp: Date.now(),
        uptime: process.uptime()
    });
});

console.log('[APP] ✅ Loaded health endpoints');

// ============================================================
// 🚀 ESSENTIAL MIDDLEWARE ONLY
// ============================================================

// Request parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
console.log('[APP] ✅ Loaded request parsers');

// CORS
app.use(cors());
console.log('[APP] ✅ Loaded CORS');

// Simple correlation ID (minimal)
app.use((req, res, next) => {
    req.correlationId = `req_${Date.now()}_${Math.random().toString(36).substring(7)}`;
    res.setHeader('x-correlation-id', req.correlationId);
    next();
});
console.log('[APP] ✅ Loaded minimal correlation ID');

// ============================================================
// 🔐 JWT AUTH MIDDLEWARE - Extracts user ID from token
// ============================================================
const JWT_SECRET = process.env.JWT_SECRET || 'super_secure_jwt_secret_change_this';

app.use('/api/payments', (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
        const token = authHeader.substring(7);
        try {
            const decoded = jwt.verify(token, JWT_SECRET);
            req.user = {
                id: decoded.userId,
                email: decoded.email,
                role: decoded.role || 'user'
            };
            console.log('[AUTH] ✅ User authenticated:', req.user.id);
        } catch (err) {
            console.log('[AUTH] ❌ Invalid token:', err.message);
        }
    } else {
        console.log('[AUTH] ⚠️ No token provided, using guest mode');
    }
    next();
});

console.log('[APP] ✅ Loaded JWT auth middleware');

// ============================================================
// 🚀 IMPORT ROUTES
// ============================================================

let paymentRoutes = null;
let webhookRoutes = null;

try {
    paymentRoutes = require('./routes/paymentRoutes');
    console.log('[APP] ✅ Loaded paymentRoutes');
} catch (e) {
    console.warn('[APP] ⚠️ paymentRoutes not found');
    paymentRoutes = (req, res) => res.status(501).json({ message: 'Payment routes not configured' });
}

try {
    webhookRoutes = require('./routes/webhookRoutes');
    console.log('[APP] ✅ Loaded webhookRoutes');
} catch (e) {
    console.warn('[APP] ⚠️ webhookRoutes not found');
    webhookRoutes = (req, res) => res.status(501).json({ message: 'Webhook routes not configured' });
}

// ============================================================
// 🚀 DATABASE METRICS ENDPOINT
// ============================================================

app.get('/db-metrics', (req, res) => {
    try {
        const metrics = getDBMetrics();
        const health = dbHealthCheck();
        res.json({
            success: true,
            database: metrics,
            health,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        console.error('[APP] DB Metrics error:', error.message);
        res.status(500).json({
            success: false,
            error: error.message,
            timestamp: new Date().toISOString()
        });
    }
});

console.log('[APP] ✅ Loaded database metrics endpoint at /db-metrics');

// ============================================================
// 🚀 API ROUTES
// ============================================================

// Payment routes (includes both payment and refund endpoints)
app.use('/api/payments', paymentRoutes);
console.log('[APP] ✅ Mounted payment routes at /api/payments (includes payment + refund)');

// Webhook routes
app.use('/api/webhooks', webhookRoutes);
console.log('[APP] ✅ Mounted webhook routes at /api/webhooks');

// Note: Refund endpoints are available under /api/payments/refund (not /api/refunds)

// ============================================================
// 🚀 SIMPLE ERROR HANDLING
// ============================================================

// 404 handler
app.use((req, res) => {
    res.status(404).json({ success: false, message: 'Route not found' });
});

// Global error handler
app.use((err, req, res, next) => {
    console.error('[APP] Error:', err.message);
    res.status(500).json({ success: false, message: 'Internal server error' });
});

// ============================================================
// EXPORTS
// ============================================================

console.log('[APP] ✅ Application configured successfully (TESTING MODE with JWT)');
module.exports = app;