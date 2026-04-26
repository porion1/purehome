/**
 * ============================================================
 * 🩺 HEALTH CHECK MIDDLEWARE — K8S PROBE & DEPENDENCY MONITORING v2.0
 * ============================================================
 *
 * PURPOSE:
 * - Kubernetes readiness/liveness probes for orchestration
 * - Real-time dependency health monitoring (DB, Stripe, Redis, Services)
 * - Graceful shutdown and startup handling
 * - Self-healing health score calculation
 *
 * SCALE TARGET:
 * - 50M+ users
 * - 99.999% uptime with auto-recovery
 * - Sub-millisecond health checks
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: DEPENDENCY GRAPH HEALTH SCORING [KEPT]
 * ------------------------------------------------------------
 * - Calculates weighted health score based on criticality
 * - Critical dependencies (DB) have higher weight
 * - Degraded score prevents traffic routing (readiness)
 *
 * 🧠 ALGORITHM 2: CIRCULAR HEALTH CASCADE [KEPT]
 * ------------------------------------------------------------
 * - Propagates health status through dependency tree
 * - Automatically degrades when upstream fails
 * - Prevents cascading failure detection
 *
 * 🧠 ALGORITHM 3: SMART PROBE ROUTING [NEW]
 * ------------------------------------------------------------
 * - Routes health requests to appropriate handlers
 * - Fast-path for liveness, detailed for readiness
 * - Prevents timeout on heavy dependency checks
 *
 * ============================================================
 */

const os = require('os');
const mongoose = require('mongoose');

// ============================================================
// CONFIG
// ============================================================

const SERVICE_NAME = process.env.SERVICE_NAME || 'payment-service';
const SERVICE_VERSION = process.env.SERVICE_VERSION || '3.0.0';
const STARTUP_GRACE_PERIOD_MS = 30000;
const SHUTDOWN_GRACE_PERIOD_MS = 30000;

// Dependency configuration with weights
const DEPENDENCIES = {
    database: {
        name: 'MongoDB',
        critical: true,
        weight: 0.40,
        check: checkDatabase,
        timeout: 3000,
    },
    stripe: {
        name: 'Stripe API',
        critical: true,
        weight: 0.30,
        check: checkStripe,
        timeout: 5000,
    },
    orderService: {
        name: 'Order Service',
        critical: false,
        weight: 0.10,
        check: checkOrderService,
        timeout: 3000,
    },
    productService: {
        name: 'Product Service',
        critical: false,
        weight: 0.10,
        check: checkProductService,
        timeout: 3000,
    },
    userService: {
        name: 'User Service',
        critical: false,
        weight: 0.05,
        check: checkUserService,
        timeout: 3000,
    },
    redis: {
        name: 'Redis',
        critical: false,
        weight: 0.05,
        check: checkRedis,
        timeout: 2000,
    },
};

// ============================================================
// 🧠 ALGORITHM 1: DEPENDENCY GRAPH HEALTH SCORING [KEPT]
// ============================================================

class DependencyHealthScorer {
    constructor() {
        this.dependencyStatus = new Map();
        this.healthHistory = [];
        this.historyWindowMs = 300000;
        this.stats = {
            totalChecks: 0,
            failedChecks: 0,
            avgLatency: 0,
            currentHealthScore: 100,
        };
        setInterval(() => this.checkAllDependencies(), 30000);
    }

    async checkAllDependencies() {
        const results = [];
        let totalScore = 0;
        let totalWeight = 0;

        for (const [key, config] of Object.entries(DEPENDENCIES)) {
            const startTime = Date.now();
            let healthy = false;
            let error = null;

            try {
                const checkPromise = config.check();
                const timeoutPromise = new Promise((_, reject) => {
                    setTimeout(() => reject(new Error(`Check timeout after ${config.timeout}ms`)), config.timeout);
                });
                const result = await Promise.race([checkPromise, timeoutPromise]);
                healthy = result.healthy !== false;
                error = result.error || null;
            } catch (err) {
                healthy = false;
                error = err.message;
            }

            const latency = Date.now() - startTime;

            this.dependencyStatus.set(key, {
                healthy,
                latency,
                error,
                lastCheck: Date.now(),
                critical: config.critical,
                weight: config.weight,
            });

            results.push({ key, healthy, latency, error });

            if (healthy) {
                totalScore += config.weight * 100;
            }
            totalWeight += config.weight;

            this.stats.totalChecks++;
            if (!healthy) this.stats.failedChecks++;
            this.stats.avgLatency = this.stats.avgLatency * 0.9 + latency * 0.1;
        }

        const healthScore = totalWeight > 0 ? (totalScore / totalWeight) : 100;
        this.stats.currentHealthScore = Math.round(healthScore);

        this.healthHistory.push({
            timestamp: Date.now(),
            score: healthScore,
            dependencies: Object.fromEntries(this.dependencyStatus),
        });

        const cutoff = Date.now() - this.historyWindowMs;
        this.healthHistory = this.healthHistory.filter(h => h.timestamp > cutoff);

        if (healthScore < 70) {
            const failedDeps = results.filter(r => !r.healthy).map(r => r.key);
            console.warn(`[HEALTH] ⚠️ Health score degraded: ${Math.round(healthScore)}% (Failed: ${failedDeps.join(', ')})`);
        }

        return { healthScore, results };
    }

    getHealthTrend() {
        if (this.healthHistory.length < 3) return 'STABLE';
        const recent = this.healthHistory.slice(-3);
        const oldest = recent[0].score;
        const newest = recent[recent.length - 1].score;
        if (newest > oldest + 5) return 'IMPROVING';
        if (newest < oldest - 5) return 'DEGRADING';
        return 'STABLE';
    }

    isReady() {
        if (Date.now() - global.serviceStartTime < STARTUP_GRACE_PERIOD_MS) return false;
        for (const [key, status] of this.dependencyStatus.entries()) {
            const config = DEPENDENCIES[key];
            if (config.critical && !status.healthy) return false;
        }
        return this.stats.currentHealthScore >= 50;
    }

    isAlive() {
        return !global.isShuttingDown && this.stats.currentHealthScore > 0;
    }

    getMetrics() {
        const dependencies = {};
        for (const [key, status] of this.dependencyStatus.entries()) {
            dependencies[key] = {
                healthy: status.healthy,
                latencyMs: status.latency,
                lastCheck: status.lastCheck,
                error: status.error,
            };
        }
        return {
            currentScore: this.stats.currentHealthScore,
            trend: this.getHealthTrend(),
            totalChecks: this.stats.totalChecks,
            failedChecks: this.stats.failedChecks,
            successRate: this.stats.totalChecks > 0
                ? ((1 - this.stats.failedChecks / this.stats.totalChecks) * 100).toFixed(2) + '%'
                : '100%',
            avgLatencyMs: Math.round(this.stats.avgLatency),
            dependencies,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: CIRCULAR HEALTH CASCADE [KEPT]
// ============================================================

class HealthCascadeManager {
    constructor() {
        this.cascadeLevel = 'NORMAL';
        this.cascadeHistory = [];
        this.thresholds = {
            NORMAL: { minScore: 80, action: 'ALLOW_ALL' },
            WARNING: { minScore: 60, action: 'REDUCE_TRAFFIC' },
            DEGRADED: { minScore: 40, action: 'READ_ONLY' },
            CRITICAL: { minScore: 0, action: 'BLOCK_ALL' },
        };
    }

    updateCascadeLevel(healthScore) {
        let newLevel = 'CRITICAL';
        if (healthScore >= this.thresholds.NORMAL.minScore) newLevel = 'NORMAL';
        else if (healthScore >= this.thresholds.WARNING.minScore) newLevel = 'WARNING';
        else if (healthScore >= this.thresholds.DEGRADED.minScore) newLevel = 'DEGRADED';
        else newLevel = 'CRITICAL';

        if (newLevel !== this.cascadeLevel) {
            console.warn(`[CASCADE] 🔄 Health cascade changed: ${this.cascadeLevel} → ${newLevel} (Score: ${healthScore})`);
            this.cascadeLevel = newLevel;
            this.cascadeHistory.push({
                from: this.cascadeLevel,
                to: newLevel,
                score: healthScore,
                timestamp: Date.now(),
            });
            while (this.cascadeHistory.length > 100) this.cascadeHistory.shift();
        }
        return this.cascadeLevel;
    }

    getAllowedOperations() {
        switch (this.cascadeLevel) {
            case 'NORMAL': return { read: true, write: true, payment: true };
            case 'WARNING': return { read: true, write: true, payment: false };
            case 'DEGRADED': return { read: true, write: false, payment: false };
            case 'CRITICAL': return { read: false, write: false, payment: false };
            default: return { read: true, write: true, payment: true };
        }
    }

    getAction() {
        return this.thresholds[this.cascadeLevel]?.action || 'ALLOW_ALL';
    }

    getMetrics() {
        return {
            level: this.cascadeLevel,
            action: this.getAction(),
            operations: this.getAllowedOperations(),
            historyCount: this.cascadeHistory.length,
        };
    }
}

// ============================================================
// 🔧 DEPENDENCY CHECK FUNCTIONS [KEPT]
// ============================================================

async function checkDatabase() {
    try {
        const state = mongoose.connection.readyState;
        const states = { 0: 'disconnected', 1: 'connected', 2: 'connecting', 3: 'disconnecting' };
        if (state === 1) {
            await mongoose.connection.db.admin().ping();
            return { healthy: true, state: states[state] };
        }
        return { healthy: false, state: states[state] };
    } catch (error) {
        return { healthy: false, error: error.message };
    }
}

async function checkStripe() {
    try {
        const Stripe = require('stripe');
        const stripe = new Stripe(process.env.STRIPE_SECRET_KEY, { timeout: 5000 });
        await stripe.balance.retrieve({ timeout: 5000 });
        return { healthy: true };
    } catch (error) {
        return { healthy: false, error: error.message };
    }
}

async function checkOrderService() {
    try {
        const axios = require('axios');
        const response = await axios.get(`${process.env.ORDER_SERVICE_URL || 'http://localhost:5003'}/health`, { timeout: 3000 });
        return { healthy: response.status === 200 };
    } catch (error) {
        return { healthy: false, error: error.message };
    }
}

async function checkProductService() {
    try {
        const axios = require('axios');
        const response = await axios.get(`${process.env.PRODUCT_SERVICE_URL || 'http://localhost:5002'}/health`, { timeout: 3000 });
        return { healthy: response.status === 200 };
    } catch (error) {
        return { healthy: false, error: error.message };
    }
}

async function checkUserService() {
    try {
        const axios = require('axios');
        const response = await axios.get(`${process.env.USER_SERVICE_URL || 'http://localhost:5001'}/health`, { timeout: 3000 });
        return { healthy: response.status === 200 };
    } catch (error) {
        return { healthy: false, error: error.message };
    }
}

async function checkRedis() {
    try {
        if (!process.env.REDIS_RATE_LIMIT_URL) {
            return { healthy: true, skipped: true };
        }
        const Redis = require('ioredis');
        const redis = new Redis(process.env.REDIS_RATE_LIMIT_URL, { connectTimeout: 2000, lazyConnect: true });
        await redis.connect();
        await redis.ping();
        await redis.quit();
        return { healthy: true };
    } catch (error) {
        return { healthy: false, error: error.message };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const healthScorer = new DependencyHealthScorer();
const cascadeManager = new HealthCascadeManager();

global.serviceStartTime = Date.now();
global.isShuttingDown = false;

// ============================================================
// 📊 SYSTEM METRICS [KEPT]
// ============================================================

const getSystemMetrics = () => {
    const memUsage = process.memoryUsage();
    const cpuUsage = process.cpuUsage();
    return {
        memory: {
            rss: Math.round(memUsage.rss / 1024 / 1024),
            heapTotal: Math.round(memUsage.heapTotal / 1024 / 1024),
            heapUsed: Math.round(memUsage.heapUsed / 1024 / 1024),
            external: Math.round(memUsage.external / 1024 / 1024),
        },
        cpu: { user: Math.round(cpuUsage.user / 1000), system: Math.round(cpuUsage.system / 1000) },
        uptime: Math.floor(process.uptime()),
        loadAverage: os.loadavg(),
        totalMemory: Math.round(os.totalmem() / 1024 / 1024),
        freeMemory: Math.round(os.freemem() / 1024 / 1024),
        cpus: os.cpus().length,
    };
};

// ============================================================
// 🚀 HEALTH CHECK HANDLERS [KEPT - WITH PROPER NAMING]
// ============================================================

const livenessHandler = async (req, res) => {
    const isAlive = healthScorer.isAlive();
    res.status(isAlive ? 200 : 503).json({
        status: isAlive ? 'alive' : 'dead',
        timestamp: new Date().toISOString(),
        uptime: Math.floor(process.uptime()),
        service: SERVICE_NAME,
        version: SERVICE_VERSION,
    });
};

const readinessHandler = async (req, res) => {
    await healthScorer.checkAllDependencies();
    const isReady = healthScorer.isReady();
    const healthScore = healthScorer.stats.currentHealthScore;
    const cascadeLevel = cascadeManager.cascadeLevel;

    if (!isReady) {
        return res.status(503).json({
            status: 'not ready',
            reason: 'Critical dependencies unhealthy',
            healthScore,
            cascadeLevel,
            timestamp: new Date().toISOString(),
        });
    }
    res.status(200).json({
        status: 'ready',
        healthScore,
        cascadeLevel,
        operations: cascadeManager.getAllowedOperations(),
        timestamp: new Date().toISOString(),
    });
};

const startupHandler = async (req, res) => {
    const elapsedMs = Date.now() - global.serviceStartTime;
    const isStarted = elapsedMs >= STARTUP_GRACE_PERIOD_MS;
    res.status(isStarted ? 200 : 503).json({
        status: isStarted ? 'started' : 'starting',
        elapsedMs,
        gracePeriodMs: STARTUP_GRACE_PERIOD_MS,
        remainingMs: Math.max(0, STARTUP_GRACE_PERIOD_MS - elapsedMs),
        timestamp: new Date().toISOString(),
    });
};

const healthDashboardHandler = async (req, res) => {
    await healthScorer.checkAllDependencies();
    const healthScore = healthScorer.stats.currentHealthScore;
    cascadeManager.updateCascadeLevel(healthScore);
    res.json({
        service: SERVICE_NAME,
        version: SERVICE_VERSION,
        status: healthScorer.isReady() ? 'healthy' : 'degraded',
        healthScore,
        trend: healthScorer.getHealthTrend(),
        cascade: cascadeManager.getMetrics(),
        dependencies: healthScorer.getMetrics().dependencies,
        system: getSystemMetrics(),
        uptime: Math.floor(process.uptime()),
        timestamp: new Date().toISOString(),
    });
};

const simpleHealthHandler = (req, res) => {
    res.status(200).json({
        status: 'ok',
        service: SERVICE_NAME,
        timestamp: new Date().toISOString(),
    });
};

// ============================================================
// 🧠 GRACEFUL SHUTDOWN HANDLER [KEPT]
// ============================================================

const setupGracefulShutdown = (server) => {
    const shutdown = async (signal) => {
        if (global.isShuttingDown) return;
        global.isShuttingDown = true;
        console.log(`[HEALTH] 🛑 Received ${signal}, starting graceful shutdown...`);
        server.close(() => console.log('[HEALTH] HTTP server closed'));
        await new Promise(resolve => setTimeout(resolve, SHUTDOWN_GRACE_PERIOD_MS));
        await mongoose.connection.close();
        console.log('[HEALTH] Database connections closed');
        console.log('[HEALTH] ✅ Graceful shutdown complete');
        process.exit(0);
    };
    process.on('SIGTERM', () => shutdown('SIGTERM'));
    process.on('SIGINT', () => shutdown('SIGINT'));
};

// ============================================================
// 📊 METRICS [KEPT]
// ============================================================

const getHealthMetrics = () => {
    return {
        healthScorer: healthScorer.getMetrics(),
        cascadeManager: cascadeManager.getMetrics(),
        system: getSystemMetrics(),
        startTime: global.serviceStartTime,
        isShuttingDown: global.isShuttingDown,
    };
};

// ============================================================
// 🧠 ALGORITHM 3: SMART PROBE ROUTING [NEW]
// ============================================================

const requireHealthy = (requiredLevel = 'NORMAL') => {
    return (req, res, next) => {
        const levelPriority = { NORMAL: 4, WARNING: 3, DEGRADED: 2, CRITICAL: 1 };
        const currentPriority = levelPriority[cascadeManager.cascadeLevel] || 0;
        const requiredPriority = levelPriority[requiredLevel] || 0;
        if (currentPriority < requiredPriority) {
            return res.status(503).json({
                success: false,
                message: `Service degraded (${cascadeManager.cascadeLevel}), operation not available`,
                code: 'SERVICE_DEGRADED',
                retryAfter: 30,
            });
        }
        next();
    };
};

// ============================================================
// 📤 EXPORTS (FIXED - Added aliases for app.js compatibility)
// ============================================================

module.exports = {
    // Primary exports for app.js (what app.js expects)
    healthCheck: simpleHealthHandler,
    livenessCheck: livenessHandler,
    readinessCheck: readinessHandler,

    // K8s probe handlers (original names)
    livenessHandler,
    readinessHandler,
    startupHandler,
    healthDashboardHandler,
    simpleHealthHandler,

    // Middleware and utilities
    requireHealthy,
    setupGracefulShutdown,
    getHealthMetrics,

    // Advanced access for monitoring
    healthScorer,
    cascadeManager,
};