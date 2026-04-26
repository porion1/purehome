const mongoose = require('mongoose');
const { EventEmitter } = require('events');

/**
 * 🚀 ALGORITHM 1: Adaptive Connection Pool Scaling (ACPS)
 * Dynamically adjusts pool size based on runtime load signals.
 */
const getDynamicPoolSize = () => {
    const base = 10;
    // FIX: Bound CPU load to reasonable range (0-100%)
    const cpuLoad = Math.min(100, process.cpuUsage().user / 1e6);
    const memoryPressure = Math.min(512, process.memoryUsage().heapUsed / 1024 / 1024);
    const loadFactor = (cpuLoad * 0.4) + (memoryPressure * 0.6);

    if (loadFactor < 50) return base * 2;
    if (loadFactor < 120) return base * 4;
    return base * 6;
};

/**
 * 🚀 ALGORITHM 2: Multi-Layer DB Resilience Engine (MDRE)
 * Lightweight circuit breaker - only for catastrophic failures
 */
class DBResilienceEngine {
    constructor() {
        this.failureCount = 0;
        this.lastFailureTime = null;
        this.circuitOpen = false;
    }

    recordFailure() {
        this.failureCount++;
        this.lastFailureTime = Date.now();

        // Only open circuit after 10 failures (much higher threshold)
        if (this.failureCount > 10) {
            this.circuitOpen = true;
            console.error('[DB] Circuit OPENED due to repeated failures');
        }
    }

    recordSuccess() {
        this.failureCount = Math.max(0, this.failureCount - 1);
        if (this.failureCount === 0) {
            this.circuitOpen = false;
        }
    }

    canAttempt() {
        if (!this.circuitOpen) return true;

        // auto-heal after 30 seconds (longer cooldown)
        const cooldown = Date.now() - this.lastFailureTime;
        if (cooldown > 30000) {
            this.circuitOpen = false;
            this.failureCount = 0;
            console.log('[DB] Circuit AUTO-HEALED');
            return true;
        }
        return false;
    }
}

const dbResilience = new DBResilienceEngine();

// ============================================================
// 🚀 ALGORITHM 3: Adaptive Hedged Reads (AHR)
// ============================================================
class HedgedReadManager {
    constructor() {
        this.latencyWindowMs = 30_000;
        this.percentile = 0.95;
        this.history = [];
        this.hedgeEnabled = true;
        this.minHedgeDelayMs = 8;
    }

    recordLatency(durationMs) {
        if (!this.hedgeEnabled) return;
        this.history.push({ duration: durationMs, timestamp: Date.now() });
        const cutoff = Date.now() - this.latencyWindowMs;
        this.history = this.history.filter(h => h.timestamp > cutoff);
    }

    getHedgeThreshold() {
        if (this.history.length < 10) return Infinity;
        const sorted = this.history.map(h => h.duration).sort((a, b) => a - b);
        const idx = Math.floor(sorted.length * this.percentile);
        return sorted[idx] || Infinity;
    }

    shouldHedge(currentLatencyMs) {
        if (!this.hedgeEnabled) return false;
        const threshold = this.getHedgeThreshold();
        if (threshold === Infinity) return false;
        return currentLatencyMs > threshold;
    }

    getHedgeDelay() {
        return this.minHedgeDelayMs;
    }
}

const hedgedReadManager = new HedgedReadManager();

// ============================================================
// 🚀 ALGORITHM 4: Workload-Aware Read Preference (WARP)
// ============================================================
class WorkloadAwareRouter {
    constructor() {
        this.replicaLagMs = 0;
        this.lastReplicaCheck = Date.now();
        this.globalReadCount = 0;
        this.globalWriteCount = 0;
        this.criticalReadsServed = 0;
    }

    recordRead(isCritical = false) {
        this.globalReadCount++;
        if (isCritical) this.criticalReadsServed++;
    }

    recordWrite() {
        this.globalWriteCount++;
    }

    async updateReplicaLag() {
        // Skip replica lag check if not using replica set
        const isReplicaSet = process.env.MONGO_URI && process.env.MONGO_URI.includes('replicaSet');
        if (!isReplicaSet) return;

        try {
            if (!mongoose.connection || mongoose.connection.readyState !== 1) return;
            const adminDb = mongoose.connection.db.admin();
            const status = await adminDb.command({ replSetGetStatus: 1 });
            if (status && status.members) {
                const primary = status.members.find(m => m.state === 1);
                const secondaries = status.members.filter(m => m.state === 2);
                if (primary && secondaries.length) {
                    const maxLag = Math.max(...secondaries.map(s => Math.abs((s.optimeDate || 0) - (primary.optimeDate || 0))));
                    this.replicaLagMs = maxLag;
                }
            }
        } catch (err) {
            // Silently ignore - not a replica set
        }
    }

    getReadPreference(isConsistencyCritical = false) {
        const readWriteRatio = this.globalWriteCount === 0 ? Infinity : this.globalReadCount / this.globalWriteCount;

        if (isConsistencyCritical) {
            return 'primary';
        }

        if (this.replicaLagMs > 2000) {
            console.warn(`[WARP] Replica lag ${this.replicaLagMs}ms → using primary`);
            return 'primary';
        }

        if (readWriteRatio > 5 && this.replicaLagMs < 500) {
            return 'secondaryPreferred';
        }

        return 'primaryPreferred';
    }

    startLagMonitor() {
        // Only start if replica set is configured
        const isReplicaSet = process.env.MONGO_URI && process.env.MONGO_URI.includes('replicaSet');
        if (isReplicaSet) {
            setInterval(async () => {
                await this.updateReplicaLag();
            }, 30000); // Check every 30 seconds instead of 5
        }
    }
}

const warpRouter = new WorkloadAwareRouter();

// ============================================================
// 🚀 INNOVATION: Tuple Return Pattern (TRP)
// ============================================================
const tuple = (error, data) => ({ error, data });
const dbTuple = async (promise) => {
    try {
        const result = await promise;
        return tuple(null, result);
    } catch (err) {
        console.error('[DB-TUPLE]', err.message);
        return tuple(err, null);
    }
};

// ============================================================
// 🔧 GLOBAL STATE (Set up ONCE, outside connectDB)
// ============================================================
let isConnecting = false;
let isReconnecting = false;
let healthCheckInterval = null;
let globalOptions = null;
let eventListenersSetup = false;
let isShuttingDown = false; // NEW: Prevent multiple shutdown attempts

// ============================================================
// 🔧 MONGOOSE EVENT LISTENERS - SET UP ONCE GLOBALLY
// ============================================================
const setupEventListeners = () => {
    if (eventListenersSetup) {
        console.log('[DB] Event listeners already set up, skipping');
        return;
    }

    console.log('[DB] Setting up global event listeners (once)');

    mongoose.connection.on('connected', () => {
        console.log('[DB] ✅ MongoDB connected');
        dbResilience.recordSuccess();
    });

    mongoose.connection.on('disconnected', () => {
        console.log('[DB] ⚠️ MongoDB disconnected');
        // FIX: Don't exit, just log and let reconnection handle it
    });

    mongoose.connection.on('error', (err) => {
        console.error('[DB] ❌ MongoDB error:', err.message);
        dbResilience.recordFailure();
        // FIX: Don't exit on error, let circuit breaker handle it
    });

    mongoose.connection.on('reconnected', () => {
        console.log('[DB] 🔄 MongoDB reconnected');
        dbResilience.recordSuccess();
    });

    eventListenersSetup = true;
};

// ============================================================
// 🔧 SIMPLE CONNECTION STATE MONITOR - NO HEAVY LOGGING
// ============================================================
let connectionStateMonitor = null;

const startConnectionStateMonitor = () => {
    if (connectionStateMonitor) return;

    connectionStateMonitor = setInterval(() => {
        const state = mongoose.connection.readyState;
        if (state === 0) {
            console.warn('[DB] Connection state: disconnected');
        }
    }, 30000); // Check every 30 seconds only
};

// ============================================================
// 🚀 MAIN DB CONNECTOR - CLEAN AND SIMPLE
// ============================================================
const connectDB = async () => {
    // FIX: Validate MONGO_URI exists
    if (!process.env.MONGO_URI) {
        const error = new Error('MONGO_URI is not defined in environment variables');
        console.error('[DB]', error.message);
        throw error;
    }

    // Prevent multiple simultaneous connection attempts
    if (isConnecting) {
        console.log('[DB] Connection already in progress, skipping...');
        return;
    }

    // Check if already connected
    if (mongoose.connection.readyState === 1) {
        console.log('[DB] Already connected, skipping...');
        return;
    }

    isConnecting = true;

    try {
        if (!dbResilience.canAttempt()) {
            throw new Error('DB circuit is OPEN (resilience protection)');
        }

        const poolSize = getDynamicPoolSize();

        globalOptions = {
            maxPoolSize: poolSize,
            minPoolSize: 5,
            serverSelectionTimeoutMS: 5000,
            socketTimeoutMS: 45000,
            family: 4,
            retryWrites: true,
            w: 'majority',
            readPreference: 'primaryPreferred',
        };

        console.log(`[DB] Connecting with pool size: ${poolSize}`);

        const start = Date.now();
        await mongoose.connect(process.env.MONGO_URI, globalOptions);
        const latency = Date.now() - start;

        hedgedReadManager.recordLatency(latency);
        dbResilience.recordSuccess();

        console.log(`[DB] Connected successfully in ${latency}ms`);

        // Start WARP lag monitor (only if replica set)
        warpRouter.startLagMonitor();

        // Expose hedge & warp utils on mongoose
        mongoose.hedgedRead = async (model, queryFn, isConsistencyCritical = false) => {
            const startQuery = Date.now();
            const readPref = warpRouter.getReadPreference(isConsistencyCritical);
            const session = await mongoose.connection.startSession();
            let hedgeTimer = null;
            let hedgePromise = null;
            let mainResolved = false;

            try {
                session.options.readPreference = readPref;
                const mainQuery = queryFn(session);

                const hedgeThresholdMs = hedgedReadManager.getHedgeThreshold();
                if (!isConsistencyCritical && hedgeThresholdMs !== Infinity && hedgedReadManager.hedgeEnabled) {
                    hedgeTimer = setTimeout(() => {
                        if (!mainResolved) {
                            console.log('[HEDGE] Issuing secondary read replica');
                            hedgePromise = queryFn(session);
                        }
                    }, hedgedReadManager.getHedgeDelay());
                }

                const result = await mainQuery;
                mainResolved = true;
                if (hedgeTimer) clearTimeout(hedgeTimer);

                const duration = Date.now() - startQuery;
                hedgedReadManager.recordLatency(duration);
                warpRouter.recordRead(isConsistencyCritical);
                return result;
            } catch (err) {
                if (hedgePromise) {
                    console.log('[HEDGE] Falling back to hedged result');
                    return hedgePromise;
                }
                throw err;
            } finally {
                await session.endSession();
            }
        };

        mongoose.tuple = dbTuple;

    } catch (error) {
        dbResilience.recordFailure();
        console.error('[DB] Connection failed:', error.message);
        throw error;
    } finally {
        isConnecting = false;
    }
};

// ============================================================
// 🚀 HEALTH CHECK - NON-AGGRESSIVE, ONLY ON ACTUAL DISCONNECT
// ============================================================
const startHealthCheck = () => {
    // Clear existing interval
    if (healthCheckInterval) {
        clearInterval(healthCheckInterval);
    }

    // Run health check every 30 seconds (less aggressive)
    healthCheckInterval = setInterval(async () => {
        // Skip if reconnecting or shutting down
        if (isReconnecting || isShuttingDown) return;

        const state = mongoose.connection.readyState;

        // ONLY treat state 0 (disconnected) as failure
        if (state === 0) {
            console.warn('[DB] Health check: disconnected');
            dbResilience.recordFailure();

            if (!isReconnecting) {
                isReconnecting = true;
                console.log('[DB] Attempting to reconnect...');

                try {
                    const reconnectOptions = globalOptions || {
                        maxPoolSize: 10,
                        minPoolSize: 5,
                        serverSelectionTimeoutMS: 5000,
                        socketTimeoutMS: 45000,
                        family: 4,
                        retryWrites: true,
                        w: 'majority',
                        readPreference: 'primaryPreferred',
                    };
                    await mongoose.connect(process.env.MONGO_URI, reconnectOptions);
                    dbResilience.recordSuccess();
                    console.log('[DB] Reconnect successful');
                } catch (err) {
                    console.error('[DB] Reconnect failed:', err.message);
                } finally {
                    isReconnecting = false;
                }
            }
        } else if (state === 1) {
            dbResilience.recordSuccess();
        }
    }, 30000);
};

// ============================================================
// 🚀 INITIALIZE (Run once at startup)
// ============================================================
const initDB = () => {
    setupEventListeners();
    startConnectionStateMonitor();
    startHealthCheck();
};

// Call init once
initDB();

// ============================================================
// 🚀 GRACEFUL SHUTDOWN - FIXED (Prevents automatic shutdown)
// ============================================================
const disconnectDB = async () => {
    if (isShuttingDown) {
        console.log('[DB] Already shutting down, skipping...');
        return;
    }

    isShuttingDown = true;
    console.log('[DB] Disconnecting gracefully...');

    if (healthCheckInterval) {
        clearInterval(healthCheckInterval);
        healthCheckInterval = null;
    }

    if (connectionStateMonitor) {
        clearInterval(connectionStateMonitor);
        connectionStateMonitor = null;
    }

    await new Promise(resolve => setTimeout(resolve, 500));

    try {
        await mongoose.disconnect();
        console.log('[DB] Disconnected gracefully');
    } catch (err) {
        console.error('[DB] Error during disconnect:', err.message);
    } finally {
        isShuttingDown = false;
    }
};

const dbTupleUtil = dbTuple;

module.exports = {
    connectDB,
    disconnectDB,
    hedgedReadManager,
    warpRouter,
    dbTuple: dbTupleUtil,
    tuple,
};