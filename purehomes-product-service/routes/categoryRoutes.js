const express = require('express');
const router = express.Router();
const categoryController = require('../controllers/categoryController');

// ----------------------------
// Algorithm 1: Basic Routing (Existing)
// ----------------------------

// ----------------------------
// Algorithm 2: Intelligent Request Throttling & Priority Queue (NEW)
// ----------------------------
// FAANG-level rate limiting that:
// 1. Dynamically adjusts limits based on system load
// 2. Prioritizes read over write operations during high traffic
// 3. Implements token bucket algorithm with adaptive fill rate
// 4. Queues excess requests with priority-based processing
// 5. Provides graceful degradation with 429 responses
// ----------------------------

class AdaptiveThrottleManager {
    constructor() {
        this.tokens = new Map(); // Token buckets per endpoint type
        this.requestQueue = []; // Priority queue for excess requests
        this.processing = false;
        this.systemLoad = 0; // 0-100 scale

        // Default limits (tokens per second)
        this.limits = {
            read: 100,    // GET requests
            write: 20,    // POST, PUT, DELETE
            admin: 50     // Future admin endpoints
        };

        // Start system load monitoring
        this.startLoadMonitoring();

        // Start queue processor
        this.startQueueProcessor();
    }

    // Monitor system load (event loop lag + memory usage)
    startLoadMonitoring() {
        setInterval(() => {
            // Measure event loop lag
            const start = Date.now();
            setImmediate(() => {
                const lag = Date.now() - start;

                // Calculate load based on lag (0-100)
                let loadFromLag = Math.min(100, (lag / 50) * 100);

                // Get memory usage
                const memoryUsed = process.memoryUsage().heapUsed / process.memoryUsage().heapTotal;
                const loadFromMemory = memoryUsed * 100;

                // Combined load (70% lag + 30% memory)
                this.systemLoad = (loadFromLag * 0.7) + (loadFromMemory * 0.3);

                // Dynamically adjust limits based on load
                if (this.systemLoad > 80) {
                    // Heavy load - reduce limits by 60%
                    this.limits.read = Math.max(20, 100 * 0.4);
                    this.limits.write = Math.max(5, 20 * 0.4);
                    console.warn(`⚠️ Heavy load (${this.systemLoad.toFixed(1)}%) - throttling enabled`);
                } else if (this.systemLoad > 60) {
                    // Moderate load - reduce limits by 30%
                    this.limits.read = Math.max(40, 100 * 0.7);
                    this.limits.write = Math.max(10, 20 * 0.7);
                } else {
                    // Normal load - restore limits
                    this.limits.read = 100;
                    this.limits.write = 20;
                }
            });
        }, 5000); // Check every 5 seconds
    }

    // Token bucket algorithm with adaptive fill rate
    getToken(endpointType) {
        const now = Date.now();
        const bucketKey = endpointType;

        if (!this.tokens.has(bucketKey)) {
            // Initialize bucket with full tokens
            this.tokens.set(bucketKey, {
                tokens: this.limits[endpointType],
                lastRefill: now,
                rate: this.limits[endpointType] / 60 // tokens per second (60s window)
            });
        }

        const bucket = this.tokens.get(bucketKey);
        const timePassed = (now - bucket.lastRefill) / 1000; // seconds

        // Refill tokens based on adaptive rate
        const refillAmount = timePassed * bucket.rate;
        bucket.tokens = Math.min(this.limits[endpointType], bucket.tokens + refillAmount);
        bucket.lastRefill = now;

        // Check if token available
        if (bucket.tokens >= 1) {
            bucket.tokens -= 1;
            return true;
        }

        return false;
    }

    // Add request to priority queue
    queueRequest(req, res, next, priority = 'normal') {
        const priorityLevel = { high: 3, normal: 2, low: 1 };
        const priorityScore = priorityLevel[priority] || 2;

        this.requestQueue.push({
            req,
            res,
            next,
            priority: priorityScore,
            timestamp: Date.now(),
            endpointType: this.getEndpointType(req.method)
        });

        // Sort queue by priority (higher first) then timestamp
        this.requestQueue.sort((a, b) => {
            if (a.priority !== b.priority) return b.priority - a.priority;
            return a.timestamp - b.timestamp;
        });

        // Send 429 if queue is too long
        if (this.requestQueue.length > 200) {
            const rejected = this.requestQueue.shift();
            if (rejected && !rejected.res.headersSent) {
                rejected.res.status(429).json({
                    error: 'Too many requests',
                    message: 'Server is under heavy load. Please try again later.',
                    retryAfter: 5,
                    queuePosition: this.requestQueue.length
                });
            }
        }
    }

    // Process queued requests
    async startQueueProcessor() {
        setInterval(async () => {
            if (this.processing || this.requestQueue.length === 0) return;

            this.processing = true;

            while (this.requestQueue.length > 0) {
                const queued = this.requestQueue[0];
                const endpointType = queued.endpointType;

                // Check if we can process this request
                if (this.getToken(endpointType)) {
                    this.requestQueue.shift(); // Remove from queue

                    // Process the request (call next middleware)
                    try {
                        queued.next();
                    } catch (error) {
                        console.error('Queue processing error:', error.message);
                        if (!queued.res.headersSent) {
                            queued.res.status(500).json({ error: 'Queue processing failed' });
                        }
                    }

                    // Small delay between processing queued requests
                    await new Promise(resolve => setTimeout(resolve, 10));
                } else {
                    // No tokens available, stop processing for now
                    break;
                }
            }

            this.processing = false;
        }, 100); // Process queue every 100ms
    }

    // Determine endpoint type based on HTTP method
    getEndpointType(method) {
        if (method === 'GET') return 'read';
        if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) return 'write';
        return 'read';
    }

    // Get request priority based on endpoint and auth
    getRequestPriority(req) {
        // Admin routes get high priority
        if (req.path.includes('/admin') || req.headers['x-admin'] === 'true') {
            return 'high';
        }

        // Read operations get normal priority
        if (req.method === 'GET') return 'normal';

        // Write operations get lower priority during high load
        if (this.systemLoad > 70) return 'low';

        return 'normal';
    }

    // Get current throttle status
    getStatus() {
        return {
            systemLoad: this.systemLoad.toFixed(1),
            limits: this.limits,
            queueLength: this.requestQueue.length,
            activeTokens: {
                read: this.tokens.get('read')?.tokens.toFixed(2) || 0,
                write: this.tokens.get('write')?.tokens.toFixed(2) || 0
            }
        };
    }
}

// Initialize throttle manager
const throttleManager = new AdaptiveThrottleManager();

// ----------------------------
// Throttling Middleware (Algorithm 2 Integration)
// ----------------------------
const adaptiveThrottle = (req, res, next) => {
    const endpointType = throttleManager.getEndpointType(req.method);
    const priority = throttleManager.getRequestPriority(req);

    // Always allow health checks and static assets
    if (req.path === '/health' || req.path.startsWith('/static')) {
        return next();
    }

    // Try to get a token
    if (throttleManager.getToken(endpointType)) {
        // Add throttle headers for monitoring
        res.setHeader('X-RateLimit-Limit', throttleManager.limits[endpointType]);
        res.setHeader('X-RateLimit-Remaining', throttleManager.tokens.get(endpointType)?.tokens.toFixed(0) || 0);
        res.setHeader('X-System-Load', throttleManager.systemLoad.toFixed(0));
        return next();
    }

    // No token available - queue the request
    throttleManager.queueRequest(req, res, next, priority);
};

// Apply throttle middleware to all category routes
router.use(adaptiveThrottle);

// ----------------------------
// Category Routes (Algorithm 1)
// ----------------------------

// GET all categories
router.get('/', categoryController.getAllCategories);

// GET category by ID
router.get('/:id', categoryController.getCategoryById);

// POST create new category
router.post('/', categoryController.createCategory);

// PUT update category
router.put('/:id', categoryController.updateCategory);

// DELETE category
router.delete('/:id', categoryController.deleteCategory);

// ----------------------------
// Throttle Status Endpoint (Admin only - monitoring)
// ----------------------------
router.get('/admin/throttle/status', (req, res) => {
    // In production, add admin authentication here
    res.json({
        algorithm: 'Intelligent Request Throttling & Priority Queue',
        status: throttleManager.getStatus(),
        features: {
            adaptiveLimits: 'Dynamic based on system load',
            priorityQueue: 'Read > Write during high traffic',
            tokenBucket: 'With adaptive fill rate',
            queueProcessing: 'Priority-based with backpressure'
        }
    });
});

// Move this BEFORE the /:id route
router.get('/health', (req, res) => {
    const status = throttleManager.getStatus();
    const healthStatus = status.systemLoad < 80 ? 'healthy' : (status.systemLoad < 90 ? 'degraded' : 'overloaded');

    res.json({
        status: healthStatus,
        systemLoad: status.systemLoad,
        queueLength: status.queueLength,
        timestamp: new Date().toISOString()
    });
});

// Keep this LAST - catches /:id after specific routes
router.get('/:id', categoryController.getCategoryById);

module.exports = router;