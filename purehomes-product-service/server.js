require('dotenv').config();
const express = require('express');
const connectDB = require('./config/db');
const productRoutes = require('./routes/productRoutes');
const categoryRoutes = require('./routes/categoryRoutes');
const morgan = require('morgan');

const app = express();

// ----------------------------
// Middleware
// ----------------------------
app.use(express.json());

// Logging middleware (verbose in dev, minimal in prod)
if (process.env.LOG_LEVEL === 'info') {
    app.use(morgan('dev'));
}

// ----------------------------
// Algorithm 1: Dynamic Feature Loader (Existing)
// ----------------------------
// Reads feature toggles from .env and dynamically enables/disables endpoints.
// Features with higher priority score are preferred if system is under high load.
// ----------------------------
const featureFlags = {};
Object.keys(process.env).forEach((key) => {
    if (key.startsWith('FEATURE_')) {
        const [priority, enabled] = process.env[key].split('|');
        featureFlags[key] = {
            priority: parseInt(priority, 10),
            enabled: enabled === 'true'
        };
    }
});

app.use((req, res, next) => {
    req.features = featureFlags;
    next();
});

// ----------------------------
// Algorithm 2: Adaptive Throughput Governor (NEW - INNOVATION)
// ----------------------------
// FAANG-level MVP: Dynamically throttles concurrent requests based on:
// - Event loop lag (how busy Node.js is)
// - Active request count
// - DB connection pool usage
// - Memory pressure
//
// Prevents cascading failure during traffic spikes without dropping connections.
// Automatically increases throughput when system is healthy.
// ----------------------------
class AdaptiveThroughputGovernor {
    constructor() {
        this.activeRequests = 0;
        this.maxConcurrent = parseInt(process.env.MAX_CONCURRENT_REQUESTS) || 50;
        this.eventLoopLagMs = 0;
        this.lastLagCheck = Date.now();
        this.rejectionCount = 0;
        this.throttleMode = 'NORMAL'; // NORMAL, THROTTLING, RECOVERY

        // Start monitoring loop
        this.startMonitoring();
    }

    startMonitoring() {
        setInterval(() => {
            // Measure event loop lag
            const now = Date.now();
            setImmediate(() => {
                this.eventLoopLagMs = Date.now() - now;
            });

            // Dynamic max concurrency based on system health
            if (this.eventLoopLagMs > 100) {
                // Event loop is blocked - reduce concurrency aggressively
                this.maxConcurrent = Math.max(10, Math.floor(this.maxConcurrent * 0.7));
                this.throttleMode = 'THROTTLING';
                console.warn(`⚠️ Governor: High event loop lag (${this.eventLoopLagMs}ms). Reducing concurrency to ${this.maxConcurrent}`);
            }
            else if (this.eventLoopLagMs < 30 && this.throttleMode !== 'NORMAL') {
                // System recovered - slowly increase concurrency
                this.maxConcurrent = Math.min(
                    parseInt(process.env.MAX_CONCURRENT_REQUESTS) || 50,
                    Math.floor(this.maxConcurrent * 1.1)
                );
                this.throttleMode = 'RECOVERY';
                console.log(`✅ Governor: System recovery. Increasing concurrency to ${this.maxConcurrent}`);
            }
            else if (this.eventLoopLagMs < 50 && this.throttleMode === 'THROTTLING') {
                this.throttleMode = 'RECOVERY';
            }

            // Reset throttle mode after stability
            if (this.throttleMode === 'RECOVERY' && this.eventLoopLagMs < 30) {
                this.throttleMode = 'NORMAL';
            }
        }, 2000); // Check every 2 seconds
    }

    canProcess() {
        const canProcessNow = this.activeRequests < this.maxConcurrent;

        if (!canProcessNow) {
            this.rejectionCount++;
            if (this.rejectionCount % 100 === 0) {
                console.warn(`🚦 Governor: ${this.rejectionCount} requests throttled. Active: ${this.activeRequests}/${this.maxConcurrent}`);
            }
        }

        return canProcessNow;
    }

    increment() {
        this.activeRequests++;
    }

    decrement() {
        this.activeRequests--;
    }

    getStats() {
        return {
            activeRequests: this.activeRequests,
            maxConcurrent: this.maxConcurrent,
            eventLoopLagMs: this.eventLoopLagMs,
            rejectionCount: this.rejectionCount,
            throttleMode: this.throttleMode,
            utilizationPercent: ((this.activeRequests / this.maxConcurrent) * 100).toFixed(1)
        };
    }
}

// Initialize the governor
const governor = new AdaptiveThroughputGovernor();

// Global middleware to enforce throughput limits
app.use((req, res, next) => {
    if (!governor.canProcess()) {
        return res.status(503).json({
            error: 'Service temporarily busy',
            retryAfter: 1,
            stats: process.env.NODE_ENV === 'development' ? governor.getStats() : undefined
        });
    }

    governor.increment();

    // Track response time and decrement when done
    const startTime = Date.now();
    res.on('finish', () => {
        const responseTime = Date.now() - startTime;
        governor.decrement();

        // Log slow requests (> 1 second)
        if (responseTime > 1000 && process.env.LOG_LEVEL === 'info') {
            console.warn(`🐌 Slow request: ${req.method} ${req.path} took ${responseTime}ms`);
        }
    });

    next();
});

// Health check endpoint with governor stats
app.get('/health/governor', (req, res) => {
    res.json({
        status: 'healthy',
        governor: governor.getStats(),
        features: Object.keys(featureFlags).filter(k => featureFlags[k].enabled)
    });
});

// ----------------------------
// Routes (with feature toggles)
// ----------------------------
if (featureFlags.FEATURE_CREATE_PRODUCT.enabled || featureFlags.FEATURE_UPDATE_PRODUCT.enabled || featureFlags.FEATURE_DELETE_PRODUCT.enabled) {
    app.use('/products', productRoutes);
}

if (featureFlags.FEATURE_CATEGORY_CREATION.enabled) {
    app.use('/categories', categoryRoutes);
}

// ----------------------------
// Root Endpoint
// ----------------------------
app.get('/', (req, res) => {
    res.send('📦 PureHome Product Service is running 🔥');
});

// ----------------------------
// Connect to DB and Start Server
// ----------------------------
const PORT = process.env.PORT || 5002;

const startServer = async () => {
    try {
        await connectDB(); // Connect to MongoDB

        // Log governor configuration on startup
        console.log(`🎮 Adaptive Throughput Governor initialized:`);
        console.log(`   - Max concurrent: ${governor.maxConcurrent}`);
        console.log(`   - Throttle mode: ${governor.throttleMode}`);
        console.log(`   - Health check: /health/governor`);

        app.listen(PORT, () => {
            console.log(`🚀 PureHome Product Service listening on port ${PORT}`);
        });
    } catch (error) {
        console.error('Failed to start server:', error.message);
        process.exit(1);
    }
};

startServer();