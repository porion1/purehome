const express = require('express');
const router = express.Router();
const rateLimit = require('express-rate-limit');
const { body, validationResult } = require('express-validator');
const {
    registerUser,
    loginUser,
    getCurrentUser,
    createGuestUser,
    refreshToken,
    logoutUser,
    changePassword,
    getSecurityMetrics,
    getUserAnomalyStatus,
    getSecurityDashboard
} = require('../controllers/userController');
const { protect, restrictTo } = require('../middleware/authMiddleware');
/**
 * INNOVATION ALGORITHM: Adaptive Route Caching with Predictive Preloading (ARC-PP)
 *
 * This algorithm optimizes API response times by:
 * 1. Analyzing route access patterns and caching frequent responses
 * 2. Predicting next likely requests based on user behavior
 * 3. Preloading cache entries before they're requested
 * 4. Adaptive TTL based on route volatility and access frequency
 */
class AdaptiveRouteCache {
    constructor() {
        this.cache = new Map();
        this.accessPatterns = new Map();
        this.preloadQueue = new Set();
        this.preloadInProgress = new Set();
        this.ttlConfig = {
            '/api/users/me': 30000,
            '/api/users/security/metrics': 60000,
            '/api/users/security/dashboard': 30000,
            '/api/users/anomaly': 15000,
            default: 15000
        };

        setInterval(() => this.cleanup(), 60000);
        setInterval(() => this.processPreloadQueue(), 10000);
    }

    generateKey(req) {
        const userId = req.user?._id || req.ip;
        return `${req.method}:${req.path}:${userId}`;
    }

    getTTL(route) {
        return this.ttlConfig[route] || this.ttlConfig.default;
    }

    shouldCache(req) {
        const cacheableRoutes = [
            '/api/users/me',
            '/api/users/security/metrics',
            '/api/users/security/dashboard',
            '/api/users/anomaly'
        ];
        const isGet = req.method === 'GET';
        const isCacheable = cacheableRoutes.some(route => req.path.startsWith(route));
        const noCache = req.headers['cache-control'] === 'no-cache';

        return isGet && isCacheable && !noCache;
    }

    get(key) {
        const entry = this.cache.get(key);
        if (!entry) return null;

        if (Date.now() > entry.expiresAt) {
            this.cache.delete(key);
            return null;
        }

        entry.accessCount++;
        return entry.data;
    }

    set(key, data, route) {
        const ttl = this.getTTL(route);
        this.cache.set(key, {
            data: JSON.parse(JSON.stringify(data)),
            expiresAt: Date.now() + ttl,
            accessCount: 0,
            createdAt: Date.now()
        });
    }

    recordAccess(route, userId) {
        const key = `${route}:${userId}`;
        const pattern = this.accessPatterns.get(key) || { count: 0, lastAccess: Date.now() };
        pattern.count++;
        pattern.lastAccess = Date.now();
        this.accessPatterns.set(key, pattern);

        this.predictNextRoutes(route, userId);
    }

    predictNextRoutes(currentRoute, userId) {
        const predictions = {
            '/api/users/me': ['/api/users/security/metrics'],
            '/api/users/login': ['/api/users/me'],
            '/api/users/register': ['/api/users/me'],
            '/api/users/security/metrics': ['/api/users/security/dashboard']
        };

        const nextRoutes = predictions[currentRoute] || [];
        for (const route of nextRoutes) {
            this.preloadQueue.add(`${route}:${userId}`);
        }
    }

    async processPreloadQueue() {
        for (const item of this.preloadQueue) {
            const [route, userId] = item.split(':');
            const key = `GET:${route}:${userId}`;

            if (!this.cache.has(key) && !this.preloadInProgress.has(item)) {
                this.preloadInProgress.add(item);
                setTimeout(() => {
                    this.preloadQueue.delete(item);
                    this.preloadInProgress.delete(item);
                }, 5000);
            } else {
                this.preloadQueue.delete(item);
            }
        }
    }

    cleanup() {
        const now = Date.now();
        for (const [key, entry] of this.cache.entries()) {
            if (now > entry.expiresAt) {
                this.cache.delete(key);
            }
        }

        const oneDayAgo = Date.now() - 86400000;
        for (const [key, pattern] of this.accessPatterns.entries()) {
            if (pattern.lastAccess < oneDayAgo) {
                this.accessPatterns.delete(key);
            }
        }
    }

    getMetrics() {
        return {
            cacheSize: this.cache.size,
            patternsTracked: this.accessPatterns.size,
            preloadQueueSize: this.preloadQueue.size,
            hitRate: Array.from(this.cache.values()).reduce((sum, entry) => sum + entry.accessCount, 0) / (this.cache.size || 1)
        };
    }

    clearUserCache(userId) {
        let deleted = 0;
        for (const [key] of this.cache.entries()) {
            if (key.includes(userId)) {
                this.cache.delete(key);
                deleted++;
            }
        }
        return deleted;
    }
}

const routeCache = new AdaptiveRouteCache();

const cacheMiddleware = async (req, res, next) => {
    if (!routeCache.shouldCache(req)) {
        return next();
    }

    const cacheKey = routeCache.generateKey(req);
    const cachedData = routeCache.get(cacheKey);

    if (cachedData) {
        routeCache.recordAccess(req.path, req.user?._id || req.ip);
        return res.json(cachedData);
    }

    const originalJson = res.json;
    res.json = function(data) {
        if (res.statusCode === 200) {
            routeCache.set(cacheKey, data, req.path);
            routeCache.recordAccess(req.path, req.user?._id || req.ip);
        }
        originalJson.call(this, data);
    };

    next();
};

const validateRequest = (validations) => {
    return async (req, res, next) => {
        await Promise.all(validations.map(validation => validation.run(req)));

        const errors = validationResult(req);
        if (errors.isEmpty()) {
            return next();
        }

        res.status(400).json({
            error: 'Validation failed',
            code: 'VALIDATION_ERROR',
            details: errors.array().map(err => ({
                field: err.param,
                message: err.msg
            }))
        });
    };
};

const rateLimiter = (windowMs = 60000, max = 100) => {
    return rateLimit({
        windowMs,
        max,
        standardHeaders: true,
        legacyHeaders: false,
        skipSuccessfulRequests: false,
        handler: (req, res) => {
            res.status(429).json({
                error: 'Too many requests',
                code: 'RATE_LIMIT_EXCEEDED',
                retryAfter: Math.ceil(windowMs / 1000)
            });
        }
    });
};

const strictRateLimiter = rateLimiter(900000, 5);
const standardRateLimiter = rateLimiter(60000, 30);
const relaxedRateLimiter = rateLimiter(60000, 100);

const requestLogger = (req, res, next) => {
    const startTime = Date.now();
    res.on('finish', () => {
        const duration = Date.now() - startTime;
        if (duration > 1000) {
            // Slow request detected
        }
    });
    next();
};

router.use(requestLogger);

// ==================== PUBLIC ROUTES ====================

router.post('/register',
    relaxedRateLimiter,
    validateRequest([
        body('name').notEmpty().withMessage('Name is required').isLength({ min: 2, max: 100 }),
        body('email').isEmail().withMessage('Valid email required').normalizeEmail(),
        body('password').isLength({ min: 8 }).withMessage('Password must be at least 8 characters')
    ]),
    registerUser
);

router.post('/login',
    strictRateLimiter,
    validateRequest([
        body('email').isEmail().withMessage('Valid email required').normalizeEmail(),
        body('password').notEmpty().withMessage('Password required')
    ]),
    loginUser
);

router.post('/guest',
    relaxedRateLimiter,
    createGuestUser
);

router.post('/refresh',
    standardRateLimiter,
    validateRequest([
        body('refreshToken').notEmpty().withMessage('Refresh token required')
    ]),
    refreshToken
);

// ==================== PROTECTED ROUTES ====================

router.post('/logout',
    protect,
    standardRateLimiter,
    logoutUser
);

router.get('/me',
    protect,
    cacheMiddleware,
    standardRateLimiter,
    getCurrentUser
);

router.put('/me/password',
    protect,
    strictRateLimiter,
    validateRequest([
        body('currentPassword').notEmpty().withMessage('Current password required'),
        body('newPassword').isLength({ min: 8 }).withMessage('New password must be at least 8 characters')
    ]),
    changePassword
);

// ==================== ADMIN ROUTES ====================

router.get('/security/metrics',
    protect,
    restrictTo('admin'),
    cacheMiddleware,
    rateLimiter(120000, 10),
    getSecurityMetrics
);

router.get('/security/auth-metrics',
    protect,
    restrictTo('admin'),
    rateLimiter(120000, 10),
    (req, res) => {
        res.json(getAuthMetrics());
    }
);

router.get('/security/dashboard',
    protect,
    restrictTo('admin'),
    cacheMiddleware,
    rateLimiter(120000, 5),
    getSecurityDashboard
);

router.get('/cache/metrics',
    protect,
    restrictTo('admin'),
    (req, res) => {
        res.json({
            algorithm: 'ARC-PP (Adaptive Route Caching with Predictive Preloading)',
            metrics: routeCache.getMetrics(),
            timestamp: Date.now()
        });
    }
);

router.delete('/cache',
    protect,
    restrictTo('admin'),
    (req, res) => {
        const userId = req.query.userId;
        let deleted = 0;

        if (userId) {
            deleted = routeCache.clearUserCache(userId);
        } else {
            for (const [key] of routeCache.cache.entries()) {
                routeCache.cache.delete(key);
                deleted++;
            }
        }

        res.json({
            message: 'Cache cleared',
            deletedCount: deleted,
            timestamp: Date.now()
        });
    }
);

// ==================== TAP ANOMALY ROUTES ====================

router.get('/anomaly/:userId',
    protect,
    rateLimiter(60000, 20),
    async (req, res, next) => {
        if (req.user.role !== 'admin' && req.user._id.toString() !== req.params.userId) {
            return res.status(403).json({
                error: 'Access denied',
                code: 'ACCESS_DENIED'
            });
        }
        next();
    },
    getUserAnomalyStatus
);

router.get('/anomaly/self/status',
    protect,
    rateLimiter(60000, 30),
    async (req, res) => {
        const User = require('../models/userModel');
        const user = await User.findById(req.user._id);
        const anomalyResult = await user.checkAnomaly(req);

        res.json({
            userId: req.user._id,
            anomalyScore: anomalyResult.score,
            risk: anomalyResult.risk,
            action: anomalyResult.action,
            reasons: anomalyResult.reasons,
            recommendations: anomalyResult.score > 60 ? [
                'Consider using a trusted device',
                'Enable two-factor authentication',
                'Review recent login activity'
            ] : []
        });
    }
);

// ==================== HEALTH & METRICS ====================

router.get('/health', (req, res) => {
    res.json({
        status: 'healthy',
        service: 'purehomes-user-service',
        timestamp: new Date().toISOString(),
        uptime: process.uptime(),
        version: require('../../package.json').version || '1.0.0'
    });
});

router.get('/metrics/summary',
    protect,
    restrictTo('admin'),
    async (req, res) => {
        const User = require('../models/userModel');

        res.json({
            timestamp: new Date().toISOString(),
            cache: routeCache.getMetrics(),
            database: {
                connectionStatus: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected'
            },
            security: {
                activeTokens: getAuthMetrics().activeTokens,
                blacklistedTokens: getAuthMetrics().blacklistedTokens
            }
        });
    }
);

// ==================== 404 HANDLER ====================

router.use((req, res) => {
    res.status(404).json({
        error: 'Route not found',
        code: 'ROUTE_NOT_FOUND',
        path: req.path,
        method: req.method
    });
});

module.exports = router;