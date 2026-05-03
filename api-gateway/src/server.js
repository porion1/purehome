const cluster = require('cluster');
const os = require('os');
const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');

// Parse worker count
let workerCount = process.env.WORKER_COUNT;
if (workerCount === 'auto' || workerCount === undefined || workerCount === '') {
    workerCount = os.cpus().length;
} else {
    workerCount = parseInt(workerCount);
    if (isNaN(workerCount) || workerCount <= 0) {
        workerCount = os.cpus().length;
    }
}

const MAX_WORKERS = Math.min(50, Math.max(1, workerCount));
const PORT = process.env.PORT || 3000;

// Service URLs from environment
const SERVICES = {
    user: process.env.USER_SERVICE_URL || 'http://user-service:5001',
    product: process.env.PRODUCT_SERVICE_URL || 'http://product-service:5002',
    order: process.env.ORDER_SERVICE_URL || 'http://order-service:5003',
    payment: process.env.PAYMENT_SERVICE_URL || 'http://payment-service:5004',
};

if (cluster.isMaster) {
    console.log(`🦁 Master ${process.pid} starting ${MAX_WORKERS} workers on port ${PORT}`);
    
    for (let i = 0; i < MAX_WORKERS; i++) {
        cluster.fork();
    }
    
    cluster.on('exit', (worker, code, signal) => {
        console.warn(`⚠️ Worker ${worker.process.pid} died. Restarting...`);
        cluster.fork();
    });
    
    process.on('SIGTERM', () => {
        console.log('🛑 Shutting down gracefully...');
        for (const id in cluster.workers) {
            cluster.workers[id].kill();
        }
        setTimeout(() => process.exit(0), 10000);
    });
    
} else {
    const app = express();
    
    // Basic middleware
    app.use(express.json());
    
    // Health endpoints
    app.get('/health/live', (req, res) => {
        res.json({ status: 'alive', uptime: process.uptime(), timestamp: new Date().toISOString() });
    });
    
    app.get('/health/ready', (req, res) => {
        res.json({ ready: true, timestamp: new Date().toISOString() });
    });
    
    app.get('/health', (req, res) => {
        res.json({ status: 'healthy', service: 'api-gateway' });
    });
    
    // ============================================
    // PROXY CONFIGURATION - WITH CORRECT PATH REWRITING
    // ============================================
    
    // User Service - expects /api/users/* (same path)
    app.use('/api/users', createProxyMiddleware({ 
        target: SERVICES.user, 
        changeOrigin: true,
        // No path rewrite needed - User Service expects /api/users/*
    }));
    
    // Product Service - expects /products/* (NO /api prefix)
    app.use('/api/products', createProxyMiddleware({ 
        target: SERVICES.product, 
        changeOrigin: true,
        pathRewrite: { '^/api/products': '' }  // /api/products → /products
    }));
    
    // Order Service - expects /api/orders/* (same path)
    app.use('/api/orders', createProxyMiddleware({ 
        target: SERVICES.order, 
        changeOrigin: true,
        // No path rewrite needed - Order Service expects /api/orders/*
    }));
    
    // Payment Service - expects /api/payments/* (same path)
    app.use('/api/payments', createProxyMiddleware({ 
        target: SERVICES.payment, 
        changeOrigin: true,
        // No path rewrite needed - Payment Service expects /api/payments/*
    }));
    
    // Metrics endpoint
    app.get('/metrics', (req, res) => {
        res.set('Content-Type', 'text/plain');
        res.send('# HELP gateway_requests_total Total requests\n# TYPE gateway_requests_total counter\ngateway_requests_total 0\n');
    });
    
    // 404 handler
    app.use((req, res) => {
        res.status(404).json({ error: 'Not Found', path: req.path });
    });
    
    // Error handler
    app.use((err, req, res, next) => {
        console.error('Gateway error:', err.message);
        res.status(500).json({ error: 'Internal Server Error', message: err.message });
    });
    
    const server = app.listen(PORT, () => {
        console.log(`✅ Worker ${process.pid} listening on port ${PORT}`);
        console.log(`📡 Routes:`);
        console.log(`   → /api/users/* → ${SERVICES.user}`);
        console.log(`   → /api/products/* → ${SERVICES.product} (rewrites /api/products → /products)`);
        console.log(`   → /api/orders/* → ${SERVICES.order}`);
        console.log(`   → /api/payments/* → ${SERVICES.payment}`);
    });
    
    process.on('SIGTERM', () => {
        console.log(`Worker ${process.pid} received SIGTERM, closing...`);
        server.close(() => process.exit(0));
    });
    
    process.on('uncaughtException', (err) => {
        console.error(`Worker ${process.pid} uncaught exception:`, err.message);
        server.close(() => process.exit(1));
    });
}

module.exports = { WORKER_COUNT: MAX_WORKERS };