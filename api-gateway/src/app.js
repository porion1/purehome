const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');

const createApp = () => {
    const app = express();
    app.use(express.json());
    return app;
};

const setupRoutes = (app) => {
    const SERVICES = {
        user: 'http://user-service:5001',
        product: 'http://product-service:5002',
        order: 'http://order-service:5003',
        payment: 'http://payment-service:5004',
    };
    
    app.use('/api/users', createProxyMiddleware({ target: SERVICES.user, changeOrigin: true }));
    
    app.use('/api/products', createProxyMiddleware({
        target: SERVICES.product,
        changeOrigin: true,
        pathRewrite: { '^/api/products': '' }
    }));
    
    app.use('/api/orders', createProxyMiddleware({ target: SERVICES.order, changeOrigin: true }));
    app.use('/api/payments', createProxyMiddleware({ target: SERVICES.payment, changeOrigin: true }));
};

const initializeApp = async () => {
    const app = createApp();
    setupRoutes(app);
    return { app, logger: console };
};

module.exports = { initializeApp };
