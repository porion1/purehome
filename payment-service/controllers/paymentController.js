/**
 * ============================================================
 * ⚡ PAYMENT CONTROLLER — CORE PAYMENT PROCESSING ENGINE v5.0
 * ============================================================
 *
 * WITH EXTENSIVE DEBUG LOGGING FOR 50M SCALE
 * ============================================================
 */

const { ensureDBConnection } = require("../utils/dbHelper");

const Payment = require('../models/paymentModel');
const Transaction = require('../models/transactionModel');
const { executeStripe } = require('../services/stripeService');
const orderBridgeService = require('../services/orderBridgeService');
const crypto = require('crypto');

const {
    PAYMENT_STATUS,
    TRANSACTION_STATUS,
    TRANSACTION_TYPE,
    ERROR_CODES,
    HTTP_STATUS,
    RISK_LEVELS,
} = require('../constants');

// ============================================================
// 🔍 EXTENSIVE DEBUG LOGGING UTILITY
// ============================================================

const DEBUG = process.env.DEBUG === 'true';
let stepCounter = 0;

const resetStepCounter = () => { stepCounter = 0; };

const logStep = (module, message, data = {}) => {
    stepCounter++;
    const stepNum = String(stepCounter).padStart(2, '0');
    console.log(`[STEP ${stepNum}] [${module}] ${message}`);
    if (DEBUG && Object.keys(data).length) {
        console.log(`[STEP ${stepNum}] [DATA]`, JSON.stringify(data, null, 2));
    }
};

const logInfo = (module, message, data = {}) => {
    console.log(`[INFO][${module}] ${message}`, Object.keys(data).length ? JSON.stringify(data) : '');
};

const logError = (module, message, error) => {
    console.error(`[ERROR][${module}] ${message}`, error.message || error);
    if (error.stack && DEBUG) {
        console.error(error.stack);
    }
};

const logWarn = (module, message, data = {}) => {
    console.warn(`[WARN][${module}] ${message}`, Object.keys(data).length ? JSON.stringify(data) : '');
};

const logSeparator = () => {
    console.log('========================================');
};

// Add timestamp to all logs
const logWithTimestamp = (level, module, message, data = {}) => {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] [${level}] [${module}] ${message}`, Object.keys(data).length ? JSON.stringify(data) : '');
};

console.log('[PAYMENT-CONTROLLER] ========================================');
console.log('[PAYMENT-CONTROLLER] PAYMENT CONTROLLER LOADED');
console.log('[PAYMENT-CONTROLLER] Debug mode:', DEBUG);
console.log('[PAYMENT-CONTROLLER] ========================================');

// ============================================================
// 🧠 ALGORITHM 1: ZIC (Zero-Conflict Idempotency Core)
// ============================================================

const idempotencyStore = new Map();

// Cleanup expired idempotency keys every hour
setInterval(() => {
    const now = Date.now();
    let cleaned = 0;
    for (const [key, value] of idempotencyStore.entries()) {
        if (now - value.createdAt > 3600000) {
            idempotencyStore.delete(key);
            cleaned++;
        }
    }
    if (cleaned > 0) {
        console.log(`[ZIC] 🧹 Cleaned ${cleaned} expired idempotency keys at ${new Date().toISOString()}`);
    }
}, 3600000);

const ZIC = {
    generateKey: (userId, orderId) => {
        console.log(`[ZIC] generateKey called: userId=${userId}, orderId=${orderId}, timestamp=${new Date().toISOString()}`);
        const key = crypto
            .createHash('sha256')
            .update(`${userId}:${orderId}`)
            .digest('hex');
        console.log(`[ZIC] Generated key: ${key.substring(0, 16)}... (full length: ${key.length})`);
        logStep('ZIC', `Generated key`, { key: key.substring(0, 16), userId, orderId });
        return key;
    },

    acquire: (key) => {
        console.log(`[ZIC] acquire called for key: ${key.substring(0, 16)}..., timestamp=${new Date().toISOString()}`);
        if (idempotencyStore.has(key)) {
            console.log(`[ZIC] ⚠️ Duplicate request blocked for key: ${key.substring(0, 16)}...`);
            logStep('ZIC', `⚠️ Duplicate request blocked`, { key: key.substring(0, 16) });
            return { allowed: false, reason: ERROR_CODES.DUPLICATE_REQUEST };
        }

        idempotencyStore.set(key, {
            status: 'LOCKED',
            createdAt: Date.now()
        });
        console.log(`[ZIC] ✅ Lock acquired for key: ${key.substring(0, 16)}..., store size: ${idempotencyStore.size}`);
        logStep('ZIC', `✅ Lock acquired`, { key: key.substring(0, 16) });
        return { allowed: true };
    },

    release: (key, status = 'COMPLETED') => {
        console.log(`[ZIC] release called for key: ${key.substring(0, 16)}..., status=${status}, timestamp=${new Date().toISOString()}`);
        if (idempotencyStore.has(key)) {
            idempotencyStore.set(key, {
                status,
                updatedAt: Date.now()
            });
            console.log(`[ZIC] 🔓 Lock released for key: ${key.substring(0, 16)}...`);
            logStep('ZIC', `🔓 Lock released`, { key: key.substring(0, 16), status });

            setTimeout(() => {
                if (idempotencyStore.get(key)?.status !== 'PROCESSING') {
                    idempotencyStore.delete(key);
                    console.log(`[ZIC] 🧹 Key deleted after timeout: ${key.substring(0, 16)}...`);
                }
            }, 3600000);
        } else {
            console.log(`[ZIC] ⚠️ Key not found for release: ${key.substring(0, 16)}...`);
        }
    }
};

// ============================================================
// 🧠 ALGORITHM 2: TLT (Temporal Ledger Tree)
// ============================================================

const buildLedgerNode = (prev, event) => {
    console.log(`[TLT] Building ledger node for event: ${event.type}, timestamp=${new Date().toISOString()}`);
    const node = {
        id: crypto.randomUUID(),
        timestamp: Date.now(),
        event,
        prevHash: prev ? prev.hash : null,
        hash: crypto
            .createHash('sha256')
            .update(JSON.stringify(event) + Date.now() + (prev?.hash || ''))
            .digest('hex')
    };
    console.log(`[TLT] Built ledger node: ${node.id.substring(0, 8)}..., hash: ${node.hash.substring(0, 16)}...`);
    logStep('TLT', `Built ledger node`, { nodeId: node.id.substring(0, 8), event: event.type });
    return node;
};

// ============================================================
// 🧠 ALGORITHM 3: ORCA (Optimistic Request Coalescing)
// ============================================================

class RequestCoalescer {
    constructor() {
        this.pendingRequests = new Map();
        this.coalesceWindowMs = 50;
        this.stats = {
            totalRequests: 0,
            coalescedRequests: 0,
            batchesProcessed: 0,
        };
        console.log('[ORCA] Request coalescer initialized at', new Date().toISOString());
        setInterval(() => this.cleanupStale(), 5000);
        logInfo('ORCA', 'Request coalescer initialized');
    }

    generateCoalesceKey(userId, orderId, amount) {
        const key = crypto.createHash('sha256').update(`${userId}:${orderId}:${amount}`).digest('hex');
        console.log(`[ORCA] Generated coalesce key: ${key.substring(0, 16)}...`);
        return key;
    }

    async coalesce(userId, orderId, amount, executeFn) {
        const key = this.generateCoalesceKey(userId, orderId, amount);
        this.stats.totalRequests++;
        console.log(`[ORCA] Coalesce called for key: ${key.substring(0, 16)}..., totalRequests=${this.stats.totalRequests}, pendingCount=${this.pendingRequests.size}`);
        logStep('ORCA', `Request for key: ${key.substring(0, 16)}`, { totalRequests: this.stats.totalRequests });

        if (this.pendingRequests.has(key)) {
            this.stats.coalescedRequests++;
            console.log(`[ORCA] 🔄 Coalescing request, coalescedRequests=${this.stats.coalescedRequests}, coalesceRate=${(this.stats.coalescedRequests / this.stats.totalRequests * 100).toFixed(1)}%`);
            logStep('ORCA', `🔄 Coalescing request`, {
                key: key.substring(0, 16),
                coalescedRate: (this.stats.coalescedRequests / this.stats.totalRequests * 100).toFixed(1) + '%'
            });
            const pending = this.pendingRequests.get(key);
            return pending.promise;
        }

        let resolve, reject;
        const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
        this.pendingRequests.set(key, { promise, resolve, reject, timestamp: Date.now() });
        console.log(`[ORCA] New pending request added, pendingCount=${this.pendingRequests.size}`);

        setTimeout(async () => {
            const pending = this.pendingRequests.get(key);
            if (!pending) return;
            this.pendingRequests.delete(key);
            this.stats.batchesProcessed++;
            console.log(`[ORCA] Processing batch, batchesProcessed=${this.stats.batchesProcessed}, batchSize=${this.stats.batchedRequests}`);

            try {
                logStep('ORCA', `🚀 Executing batch`, { key: key.substring(0, 16) });
                const result = await executeFn();
                pending.resolve(result);
                console.log(`[ORCA] Batch executed successfully`);
            } catch (error) {
                logError('ORCA', `Batch execution failed`, error);
                console.error(`[ORCA] Batch execution failed: ${error.message}`);
                pending.reject(error);
            }
        }, this.coalesceWindowMs);

        return promise;
    }

    cleanupStale() {
        const now = Date.now();
        let cleaned = 0;
        for (const [key, pending] of this.pendingRequests.entries()) {
            if (now - pending.timestamp > 10000) {
                pending.reject(new Error('Request coalescing timeout'));
                this.pendingRequests.delete(key);
                cleaned++;
            }
        }
        if (cleaned > 0) {
            console.log(`[ORCA] 🧹 Cleaned ${cleaned} stale requests at ${new Date().toISOString()}`);
            logStep('ORCA', `🧹 Cleaned ${cleaned} stale requests`);
        }
    }

    getStats() {
        return {
            totalRequests: this.stats.totalRequests,
            coalescedRequests: this.stats.coalescedRequests,
            coalesceRate: this.stats.totalRequests > 0 ? ((this.stats.coalescedRequests / this.stats.totalRequests) * 100).toFixed(1) + '%' : '0%',
            batchesProcessed: this.stats.batchesProcessed,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 4: PHOENIX (Predictive Health Orchestration)
// ============================================================

class PhoenixCircuitBreaker {
    constructor() {
        this.serviceHealth = new Map();
        this.circuitStates = new Map();
        this.failureThresholds = {
            stripe: { failures: 5, windowMs: 60000, recoveryMs: 30000 },
            orderBridge: { failures: 3, windowMs: 30000, recoveryMs: 15000 },
            database: { failures: 10, windowMs: 60000, recoveryMs: 60000 },
        };
        this.stats = {
            totalFailures: 0,
            circuitsOpened: 0,
            circuitsClosed: 0,
            lastHealthCheck: Date.now(),
        };
        console.log('[PHOENIX] Circuit breaker initialized at', new Date().toISOString());
        setInterval(() => this.healthCheck(), 10000);
        logInfo('PHOENIX', 'Circuit breaker initialized');
    }

    recordFailure(service, error) {
        console.log(`[PHOENIX] ❌ Recording failure for ${service}: ${error.message}, timestamp=${new Date().toISOString()}`);
        logStep('PHOENIX', `❌ Recording failure`, { service, error: error.message });

        if (!this.serviceHealth.has(service)) {
            this.serviceHealth.set(service, { failures: [], lastFailure: null, consecutiveFailures: 0 });
        }
        const health = this.serviceHealth.get(service);
        health.failures.push({ timestamp: Date.now(), error: error.message });
        health.lastFailure = Date.now();
        health.consecutiveFailures++;
        console.log(`[PHOENIX] ${service} failure count: ${health.failures.length}, consecutive: ${health.consecutiveFailures}`);

        const threshold = this.failureThresholds[service];
        if (threshold) {
            const cutoff = Date.now() - threshold.windowMs;
            health.failures = health.failures.filter(f => f.timestamp > cutoff);
        }
        this.stats.totalFailures++;
        this.checkCircuitBreaker(service);
    }

    recordSuccess(service) {
        console.log(`[PHOENIX] ✅ Recording success for ${service}, timestamp=${new Date().toISOString()}`);
        logStep('PHOENIX', `✅ Recording success`, { service });

        if (!this.serviceHealth.has(service)) {
            this.serviceHealth.set(service, { failures: [], lastFailure: null, consecutiveFailures: 0 });
        }
        const health = this.serviceHealth.get(service);
        const oldConsecutive = health.consecutiveFailures;
        health.consecutiveFailures = 0;
        console.log(`[PHOENIX] ${service} consecutive failures reset from ${oldConsecutive} to 0`);

        if (this.circuitStates.get(service) === 'HALF_OPEN') {
            console.log(`[PHOENIX] ${service} circuit in HALF_OPEN, attempting to close`);
            this.closeCircuit(service);
        }
    }

    checkCircuitBreaker(service) {
        const health = this.serviceHealth.get(service);
        const threshold = this.failureThresholds[service];
        if (!threshold) return;

        const currentState = this.circuitStates.get(service) || 'CLOSED';
        console.log(`[PHOENIX] Checking circuit for ${service}: currentState=${currentState}, failures=${health.failures.length}, threshold=${threshold.failures}`);

        if (currentState === 'CLOSED' && health.failures.length >= threshold.failures) {
            console.log(`[PHOENIX] Circuit breaker threshold reached for ${service} (${health.failures.length}/${threshold.failures}), opening circuit`);
            this.openCircuit(service);
        }
    }

    openCircuit(service) {
        this.circuitStates.set(service, 'OPEN');
        this.stats.circuitsOpened++;
        console.log(`[PHOENIX] 🔴 Circuit OPENED for ${service} at ${new Date().toISOString()} (total opens: ${this.stats.circuitsOpened})`);
        logError('PHOENIX', `🔴 Circuit OPENED for ${service}`, new Error(`After ${this.failureThresholds[service]?.failures} failures`));

        const threshold = this.failureThresholds[service];
        setTimeout(() => {
            if (this.circuitStates.get(service) === 'OPEN') {
                this.circuitStates.set(service, 'HALF_OPEN');
                console.log(`[PHOENIX] 🔄 Circuit HALF_OPEN for ${service} at ${new Date().toISOString()} - testing recovery`);
                logInfo('PHOENIX', `🔄 Circuit HALF_OPEN for ${service} - testing recovery`);
            }
        }, threshold.recoveryMs);
    }

    closeCircuit(service) {
        this.circuitStates.set(service, 'CLOSED');
        this.stats.circuitsClosed++;
        this.serviceHealth.set(service, { failures: [], lastFailure: null, consecutiveFailures: 0 });
        console.log(`[PHOENIX] ✅ Circuit CLOSED for ${service} at ${new Date().toISOString()} (total closes: ${this.stats.circuitsClosed})`);
        logInfo('PHOENIX', `✅ Circuit CLOSED for ${service}`);
    }

    isAllowed(service) {
        const state = this.circuitStates.get(service) || 'CLOSED';
        console.log(`[PHOENIX] isAllowed for ${service}: state=${state}, timestamp=${new Date().toISOString()}`);
        if (state === 'OPEN') return false;
        if (state === 'HALF_OPEN') {
            const allowed = Math.random() < 0.1;
            console.log(`[PHOENIX] HALF_OPEN test for ${service}: ${allowed ? 'allowed (10% chance)' : 'blocked (90% chance)'}`);
            logStep('PHOENIX', `Half-open test for ${service}: ${allowed ? 'allowed' : 'blocked'}`);
            return allowed;
        }
        return true;
    }

    getHealthScore(service) {
        const health = this.serviceHealth.get(service);
        const state = this.circuitStates.get(service) || 'CLOSED';
        if (state === 'OPEN') {
            console.log(`[PHOENIX] getHealthScore for ${service}: 0 (circuit OPEN)`);
            return 0;
        }
        if (state === 'HALF_OPEN') {
            console.log(`[PHOENIX] getHealthScore for ${service}: 30 (circuit HALF_OPEN)`);
            return 30;
        }
        if (!health || health.failures.length === 0) {
            console.log(`[PHOENIX] getHealthScore for ${service}: 100 (no failures)`);
            return 100;
        }

        const threshold = this.failureThresholds[service];
        const failureRate = health.failures.length / (threshold?.failures || 10);
        const score = Math.max(0, Math.min(100, 100 - (failureRate * 100)));
        console.log(`[PHOENIX] getHealthScore for ${service}: ${score} (failures=${health.failures.length}, failureRate=${failureRate.toFixed(2)})`);
        return score;
    }

    async healthCheck() {
        const services = ['stripe', 'orderBridge', 'database'];
        const results = {};
        for (const service of services) {
            results[service] = {
                allowed: this.isAllowed(service),
                healthScore: this.getHealthScore(service),
                circuitState: this.circuitStates.get(service) || 'CLOSED',
            };
        }
        this.stats.lastHealthCheck = Date.now();
        console.log(`[PHOENIX] Health check completed at ${new Date(this.stats.lastHealthCheck).toISOString()}`);
        return results;
    }

    getMetrics() {
        return {
            stats: this.stats,
            circuits: {
                stripe: this.circuitStates.get('stripe') || 'CLOSED',
                orderBridge: this.circuitStates.get('orderBridge') || 'CLOSED',
                database: this.circuitStates.get('database') || 'CLOSED',
            },
            healthScores: {
                stripe: this.getHealthScore('stripe'),
                orderBridge: this.getHealthScore('orderBridge'),
                database: this.getHealthScore('database'),
            },
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 5: SHADOW (Synchronous Hedged Adaptive Delivery)
// ============================================================

class ShadowHedgedExecutor {
    constructor() {
        this.hedgeDelayMs = 150;
        this.hedgeStats = { totalHedges: 0, successfulHedges: 0, hedgeSavingsMs: 0 };
        console.log('[SHADOW] Hedged executor initialized at', new Date().toISOString());
        logInfo('SHADOW', 'Hedged executor initialized');
    }

    async executeWithHedge(primaryFn, hedgeFn, timeoutMs = 5000) {
        const startTime = Date.now();
        let primaryCompleted = false;
        let hedgeTimeout = null;
        let hedgePromise = null;
        console.log(`[SHADOW] executeWithHedge started at ${new Date().toISOString()}, hedgeDelayMs=${this.hedgeDelayMs}, timeoutMs=${timeoutMs}`);

        const primaryPromise = (async () => {
            try {
                console.log(`[SHADOW] Primary execution started at ${new Date().toISOString()}`);
                const result = await primaryFn();
                primaryCompleted = true;
                if (hedgeTimeout) clearTimeout(hedgeTimeout);
                const latency = Date.now() - startTime;
                console.log(`[SHADOW] Primary execution completed in ${latency}ms at ${new Date().toISOString()}`);
                logStep('SHADOW', `Primary request completed`, { latency: Date.now() - startTime });
                return { hedged: false, result };
            } catch (error) {
                console.log(`[SHADOW] Primary execution failed: ${error.message} at ${new Date().toISOString()}`);
                logStep('SHADOW', `Primary request failed`, { error: error.message });
                if (hedgePromise) {
                    console.log(`[SHADOW] Waiting for hedge result`);
                    const hedgeResult = await hedgePromise;
                    return hedgeResult;
                }
                throw error;
            }
        })();

        hedgePromise = new Promise(async (resolve) => {
            await new Promise(resolve => { hedgeTimeout = setTimeout(resolve, this.hedgeDelayMs); });
            if (primaryCompleted) {
                console.log(`[SHADOW] Hedge cancelled - primary already completed after ${Date.now() - startTime}ms`);
                return;
            }

            this.hedgeStats.totalHedges++;
            const hedgeStartTime = Date.now();
            console.log(`[SHADOW] 🚀 Launching hedge request (#${this.hedgeStats.totalHedges}) at ${new Date().toISOString()}`);
            logStep('SHADOW', `🚀 Launching hedge request (#${this.hedgeStats.totalHedges})`);

            try {
                const result = await hedgeFn();
                const hedgeLatency = Date.now() - hedgeStartTime;
                this.hedgeStats.successfulHedges++;
                this.hedgeStats.hedgeSavingsMs += hedgeLatency;
                console.log(`[SHADOW] Hedge successful in ${hedgeLatency}ms (savings: ${this.hedgeStats.hedgeSavingsMs}ms total)`);
                logInfo('SHADOW', `Hedge successful`, { latency: hedgeLatency, savings: hedgeLatency });
                resolve({ hedged: true, result, latency: hedgeLatency });
            } catch (error) {
                console.log(`[SHADOW] Hedge failed: ${error.message}`);
                logStep('SHADOW', `Hedge failed`, { error: error.message });
                resolve({ hedged: true, error });
            }
        });

        return Promise.race([primaryPromise, hedgePromise]);
    }

    getStats() {
        return {
            totalHedges: this.hedgeStats.totalHedges,
            successRate: this.hedgeStats.totalHedges > 0 ? ((this.hedgeStats.successfulHedges / this.hedgeStats.totalHedges) * 100).toFixed(1) + '%' : 'N/A',
            avgSavingsMs: this.hedgeStats.successfulHedges > 0 ? Math.round(this.hedgeStats.hedgeSavingsMs / this.hedgeStats.successfulHedges) : 0,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 6: SENTINEL (Transaction Coordinator)
// ============================================================

class SentinelTransactionCoordinator {
    constructor() {
        this.pendingTransactions = new Map();
        this.deadLetterQueue = [];
        this.retryDelays = [1000, 2000, 4000, 8000, 16000];
        this.maxRetries = 5;
        this.stats = { totalTransactions: 0, completedTransactions: 0, failedTransactions: 0, deadLettered: 0 };
        console.log('[SENTINEL] Transaction coordinator initialized at', new Date().toISOString());
        setInterval(() => this.processRetryQueue(), 10000);
        logInfo('SENTINEL', 'Transaction coordinator initialized');
    }

    async createTransaction(orderId, paymentId, userId, amount) {
        const transactionId = crypto.randomUUID();
        console.log(`[SENTINEL] createTransaction: orderId=${orderId}, paymentId=${paymentId}, userId=${userId}, amount=${amount}, timestamp=${new Date().toISOString()}`);
        const transaction = {
            id: transactionId, orderId, paymentId, userId, amount,
            status: TRANSACTION_STATUS.PENDING,
            retryCount: 0,
            createdAt: Date.now(),
            syncStatus: 'PENDING',
        };
        this.pendingTransactions.set(transactionId, transaction);
        this.stats.totalTransactions++;
        console.log(`[SENTINEL] Transaction created: ${transactionId.substring(0, 8)}..., pendingCount=${this.pendingTransactions.size}`);

        logStep('SENTINEL', `Transaction created`, { transactionId: transactionId.substring(0, 8), orderId, amount });

        try {
            await Transaction.createSafe({
                orderId, paymentId, userId, amount,
                type: TRANSACTION_TYPE.PAYMENT,
                status: TRANSACTION_STATUS.PENDING,
                idempotencyKey: `txn_${transactionId}`,
            });
            console.log(`[SENTINEL] Transaction record saved to DB: ${transactionId.substring(0, 8)}...`);
            logStep('SENTINEL', `Transaction record saved to DB`, { transactionId: transactionId.substring(0, 8) });
        } catch (error) {
            console.error(`[SENTINEL] Failed to save transaction record: ${error.message}`);
            logError('SENTINEL', 'Failed to save transaction record', error);
        }

        return transaction;
    }

    async completeTransaction(transactionId, result) {
        console.log(`[SENTINEL] completeTransaction: ${transactionId.substring(0, 8)}..., timestamp=${new Date().toISOString()}`);
        const transaction = this.pendingTransactions.get(transactionId);
        if (!transaction) {
            console.log(`[SENTINEL] Transaction not found: ${transactionId.substring(0, 8)}...`);
            return null;
        }

        transaction.status = TRANSACTION_STATUS.SUCCEEDED;
        transaction.completedAt = Date.now();
        transaction.result = result;
        this.pendingTransactions.delete(transactionId);
        this.stats.completedTransactions++;
        const duration = transaction.completedAt - transaction.createdAt;
        console.log(`[SENTINEL] Transaction completed in ${duration}ms, completedCount=${this.stats.completedTransactions}`);

        logStep('SENTINEL', `Transaction completed`, { transactionId: transactionId.substring(0, 8), duration });

        try {
            const txnRecord = await Transaction.findOne({ idempotencyKey: `txn_${transactionId}` });
            if (txnRecord) {
                txnRecord.markSucceeded();
                await txnRecord.save();
                console.log(`[SENTINEL] Transaction record updated to SUCCEEDED`);
            }
        } catch (error) {
            console.error(`[SENTINEL] Failed to update transaction record: ${error.message}`);
            logError('SENTINEL', 'Failed to update transaction record', error);
        }

        return transaction;
    }

    async failTransaction(transactionId, error) {
        console.log(`[SENTINEL] failTransaction: ${transactionId.substring(0, 8)}..., error: ${error.message}, timestamp=${new Date().toISOString()}`);
        const transaction = this.pendingTransactions.get(transactionId);
        if (!transaction) {
            console.log(`[SENTINEL] Transaction not found: ${transactionId.substring(0, 8)}...`);
            return null;
        }

        transaction.status = TRANSACTION_STATUS.FAILED;
        transaction.error = error;
        transaction.failedAt = Date.now();

        if (transaction.retryCount < this.maxRetries) {
            transaction.retryCount++;
            transaction.nextRetry = Date.now() + this.retryDelays[transaction.retryCount - 1];
            this.stats.failedTransactions++;
            console.log(`[SENTINEL] Transaction failed, retry scheduled: attempt ${transaction.retryCount}/${this.maxRetries}, nextRetry in ${this.retryDelays[transaction.retryCount - 1]}ms`);
            logStep('SENTINEL', `Transaction failed, retry scheduled`, {
                transactionId: transactionId.substring(0, 8),
                retryCount: transaction.retryCount,
                nextRetry: new Date(transaction.nextRetry).toISOString()
            });
            return transaction;
        }

        this.pendingTransactions.delete(transactionId);
        this.addToDeadLetter(transaction, error);
        this.stats.deadLettered++;
        console.log(`[SENTINEL] Transaction dead-lettered after ${this.maxRetries} retries, deadLetterSize=${this.deadLetterQueue.length}`);

        logError('SENTINEL', `Transaction dead-lettered after ${this.maxRetries} retries`, error);

        try {
            const txnRecord = await Transaction.findOne({ idempotencyKey: `txn_${transactionId}` });
            if (txnRecord) txnRecord.markFailed(error.message);
            console.log(`[SENTINEL] Transaction record marked as FAILED`);
        } catch (err) {
            console.error(`[SENTINEL] Failed to update transaction record: ${err.message}`);
            logError('SENTINEL', 'Failed to update transaction record', err);
        }

        return transaction;
    }

    addToDeadLetter(transaction, error) {
        this.deadLetterQueue.push({ ...transaction, error: error.message, deadLetteredAt: Date.now() });
        console.log(`[SENTINEL] Added to dead letter queue, new size=${this.deadLetterQueue.length}`);
        logStep('SENTINEL', `Added to dead letter queue`, { queueSize: this.deadLetterQueue.length });
    }

    async processRetryQueue() {
        const now = Date.now();
        let retried = 0;
        for (const [id, transaction] of this.pendingTransactions.entries()) {
            if (transaction.nextRetry && transaction.nextRetry <= now && transaction.retryCount < this.maxRetries) {
                transaction.status = 'PENDING';
                transaction.nextRetry = null;
                this.pendingTransactions.set(id, transaction);
                retried++;
                console.log(`[SENTINEL] Retrying transaction: ${id.substring(0, 8)}..., attempt ${transaction.retryCount}`);
            }
        }
        if (retried > 0) {
            console.log(`[SENTINEL] Retried ${retried} transactions from queue at ${new Date().toISOString()}`);
            logInfo('SENTINEL', `Retried ${retried} transactions from queue`);
        }
    }

    getMetrics() {
        return {
            totalTransactions: this.stats.totalTransactions,
            completedTransactions: this.stats.completedTransactions,
            failedTransactions: this.stats.failedTransactions,
            deadLettered: this.stats.deadLettered,
            pendingTransactions: this.pendingTransactions.size,
            deadLetterQueueSize: this.deadLetterQueue.length,
            successRate: this.stats.totalTransactions > 0 ? ((this.stats.completedTransactions / this.stats.totalTransactions) * 100).toFixed(2) + '%' : 'N/A',
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 7: GUARDIAN (Pre-auth Validation & Risk Scoring)
// ============================================================

class GuardianPreAuthValidator {
    constructor() {
        this.cache = new Map();
        this.cacheTTL = 300000;
        this.stats = {
            totalValidations: 0,
            passedValidations: 0,
            failedValidations: 0,
            cacheHits: 0,
        };
        console.log('[GUARDIAN] Pre-auth validator initialized at', new Date().toISOString());
        setInterval(() => this.cleanupCache(), 60000);
        logInfo('GUARDIAN', 'Pre-auth validator initialized');
    }

    async validateUser(userId, authToken, amount) {
        console.log(`[GUARDIAN] validateUser: userId=${userId}, amount=${amount}, hasToken=${!!authToken}, timestamp=${new Date().toISOString()}`);
        const cacheKey = `${userId}:${Math.floor(Date.now() / this.cacheTTL)}`;

        if (this.cache.has(cacheKey)) {
            this.stats.cacheHits++;
            const cached = this.cache.get(cacheKey);
            if (cached.amount === amount) {
                console.log(`[GUARDIAN] Cache HIT for user ${userId}, returning cached result`);
                return cached.result;
            }
            console.log(`[GUARDIAN] Cache MISS for user ${userId} (amount mismatch: cached=${cached.amount}, request=${amount})`);
        } else {
            console.log(`[GUARDIAN] Cache MISS for user ${userId} (no cache entry)`);
        }

        this.stats.totalValidations++;
        console.log(`[GUARDIAN] Calling User Service for user ${userId}...`);

        try {
            const userServiceClient = require('../integrations/userServiceClient');
            const riskScore = await userServiceClient.getPaymentRiskScore(userId, amount, authToken);
            console.log(`[GUARDIAN] User Service response: riskScore=${riskScore.riskScore}, riskLevel=${riskScore.riskLevel}, recommendation=${riskScore.recommendation}`);

            const result = {
                valid: riskScore.recommendation !== 'BLOCK',
                riskScore: riskScore.riskScore,
                riskLevel: riskScore.riskLevel,
                recommendation: riskScore.recommendation,
                requires2FA: riskScore.recommendation === 'REQUIRE_2FA',
                requiresVerification: riskScore.recommendation === 'REQUIRE_VERIFICATION',
            };

            if (result.valid) {
                this.stats.passedValidations++;
                console.log(`[GUARDIAN] User validation PASSED for ${userId}`);
            } else {
                this.stats.failedValidations++;
                console.log(`[GUARDIAN] User validation FAILED for ${userId}`);
                logWarn('GUARDIAN', `User validation failed`, { userId, riskScore: riskScore.riskScore, reason: riskScore.reason });
            }

            this.cache.set(cacheKey, { result, amount, timestamp: Date.now() });
            console.log(`[GUARDIAN] Cached result for user ${userId}, cache size=${this.cache.size}`);
            return result;
        } catch (error) {
            console.error(`[GUARDIAN] User validation error: ${error.message}`);
            logError('GUARDIAN', 'User validation failed', error);
            return {
                valid: true,
                riskScore: 50,
                riskLevel: 'MEDIUM',
                recommendation: 'ALLOW',
                requires2FA: false,
                requiresVerification: false,
                error: error.message,
            };
        }
    }

    async validateStock(orderId, items) {
        console.log(`[GUARDIAN] validateStock: orderId=${orderId}, itemsCount=${items?.length}, timestamp=${new Date().toISOString()}`);
        try {
            const productServiceClient = require('../integrations/productServiceClient');
            const reservations = [];
            for (let i = 0; i < items.length; i++) {
                const item = items[i];
                console.log(`[GUARDIAN] Reserving stock for product ${i+1}/${items.length}: productId=${item.productId}, variantId=${item.variantId}, quantity=${item.quantity}`);
                const reservation = await productServiceClient.reserveStock({
                    productId: item.productId,
                    variantId: item.variantId,
                    quantity: item.quantity,
                    orderId: orderId,
                    ttlMs: 600000,
                });
                reservations.push(reservation);
                console.log(`[GUARDIAN] Reservation ${i+1} created: ${reservation.reservationId}, expires in ${reservation.expiresInSeconds}s`);
            }
            const allReserved = reservations.every(r => r.success);
            console.log(`[GUARDIAN] Stock validation result: ${allReserved ? 'PASSED' : 'FAILED'} (${reservations.filter(r => r.success).length}/${reservations.length} successful)`);
            return {
                valid: allReserved,
                reservations: reservations,
                message: allReserved ? 'Stock reserved successfully' : 'Some items out of stock',
            };
        } catch (error) {
            console.error(`[GUARDIAN] Stock validation error: ${error.message}`);
            logError('GUARDIAN', 'Stock validation failed', error);
            return {
                valid: false,
                reservations: [],
                message: 'Stock validation service unavailable',
                error: error.message,
            };
        }
    }

    async validateOrder(orderId, authToken) {
        console.log(`[GUARDIAN] validateOrder: orderId=${orderId}, hasToken=${!!authToken}, timestamp=${new Date().toISOString()}`);
        try {
            const orderServiceClient = require('../integrations/orderServiceClient');
            console.log(`[GUARDIAN] Calling Order Service for order ${orderId}...`);
            const order = await orderServiceClient.getOrder(orderId, authToken);
            console.log(`[GUARDIAN] Order Service response: orderId=${orderId}, status=${order?.status}, hasReservations=${!!order?.reservations?.length}`);

            if (!order) {
                console.log(`[GUARDIAN] Order not found: ${orderId}`);
                return { valid: false, message: 'Order not found', order: null };
            }
            const validStatuses = ['pending_payment', 'created'];
            const isValid = validStatuses.includes(order.status);
            console.log(`[GUARDIAN] Order validation result: ${isValid ? 'VALID' : 'INVALID'} (status=${order.status}, expected one of [${validStatuses.join(', ')}])`);

            // Log reservation details for debugging
            if (order.reservations && order.reservations.length > 0) {
                console.log(`[GUARDIAN] Order has ${order.reservations.length} reservation(s):`);
                order.reservations.forEach((res, idx) => {
                    console.log(`[GUARDIAN]   Reservation ${idx + 1}: id=${res.reservationId}, expiresAt=${res.expiresAt}`);
                });
            } else {
                console.log(`[GUARDIAN] Order has NO reservations`);
            }

            return {
                valid: isValid,
                message: isValid ? 'Order valid' : `Order status is ${order.status}, expected pending_payment`,
                order: order,
            };
        } catch (error) {
            console.error(`[GUARDIAN] Order validation error: ${error.message}`);
            logError('GUARDIAN', 'Order validation failed', error);
            return { valid: false, message: 'Order service unavailable', order: null, error: error.message };
        }
    }

    cleanupCache() {
        const now = Date.now();
        let cleaned = 0;
        for (const [key, value] of this.cache.entries()) {
            if (now - value.timestamp > this.cacheTTL) {
                this.cache.delete(key);
                cleaned++;
            }
        }
        if (cleaned > 0) {
            console.log(`[GUARDIAN] 🧹 Cleaned ${cleaned} expired cache entries at ${new Date().toISOString()}`);
            logStep('GUARDIAN', `🧹 Cleaned ${cleaned} expired cache entries`);
        }
    }

    getMetrics() {
        return {
            totalValidations: this.stats.totalValidations,
            passedValidations: this.stats.passedValidations,
            failedValidations: this.stats.failedValidations,
            passRate: this.stats.totalValidations > 0 ? ((this.stats.passedValidations / this.stats.totalValidations) * 100).toFixed(1) + '%' : 'N/A',
            cacheHits: this.stats.cacheHits,
            cacheSize: this.cache.size,
        };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

console.log('[PAYMENT-CONTROLLER] Initializing algorithms at', new Date().toISOString());

const requestCoalescer = new RequestCoalescer();
const phoenixBreaker = new PhoenixCircuitBreaker();
const shadowExecutor = new ShadowHedgedExecutor();
const sentinelCoordinator = new SentinelTransactionCoordinator();
const guardianValidator = new GuardianPreAuthValidator();

console.log('[PAYMENT-CONTROLLER] All algorithms initialized successfully');

// ============================================================
// 🚀 HELPER: Extract Auth Token from Request
// ============================================================

const getAuthToken = (req) => {
    const authHeader = req.headers.authorization;
    console.log(`[AUTH] getAuthToken: header present=${!!authHeader}, timestamp=${new Date().toISOString()}`);
    if (authHeader && authHeader.startsWith('Bearer ')) {
        const token = authHeader.substring(7);
        console.log(`[AUTH] Token extracted (first 20 chars): ${token.substring(0, 20)}..., length=${token.length}`);
        return token;
    }
    console.log(`[AUTH] No valid Bearer token found`);
    return null;
};

// ============================================================
// 🚀 ENHANCED STRIPE EXECUTION
// ============================================================

const safeStripeExecute = async (fn, options = {}) => {
    console.log(`[STRIPE] safeStripeExecute called with options:`, JSON.stringify(options), `timestamp=${new Date().toISOString()}`);
    logStep('STRIPE', `Checking circuit breaker`);
    if (!phoenixBreaker.isAllowed('stripe')) {
        console.log(`[STRIPE] Circuit is OPEN - rejecting request`);
        logError('STRIPE', 'Circuit is OPEN - rejecting request', new Error('Stripe circuit open'));
        throw new Error('Stripe circuit is OPEN - service degraded');
    }
    try {
        console.log(`[STRIPE] Executing Stripe operation...`);
        logStep('STRIPE', `Executing Stripe operation`);
        const result = await executeStripe(fn, options);
        phoenixBreaker.recordSuccess('stripe');
        console.log(`[STRIPE] ✅ Stripe operation successful`);
        logStep('STRIPE', `✅ Stripe operation successful`);
        return result;
    } catch (error) {
        console.log(`[STRIPE] ❌ Stripe operation failed: ${error.message}`);
        phoenixBreaker.recordFailure('stripe', error);
        logError('STRIPE', `❌ Stripe operation failed`, error);
        throw error;
    }
};

// ============================================================
// 🧪 TEST ENDPOINTS (For Debugging)
// ============================================================

const testPing = async (req, res) => {
    console.log('[TEST] testPing called at', new Date().toISOString());
    return res.json({ success: true, message: 'pong', timestamp: Date.now() });
};

const testStripeDirect = async (req, res) => {
    console.log('[TEST] testStripeDirect called at', new Date().toISOString());
    logSeparator();
    logInfo('TEST', '💳 Direct Stripe API test started');
    logStep('TEST', 'Creating test payment intent directly');

    try {
        logStep('TEST', 'Calling Stripe API via executeStripe');
        const result = await executeStripe(async (stripe) => {
            logStep('TEST', 'Inside Stripe callback - creating payment intent');
            const intent = await stripe.paymentIntents.create({
                amount: 1000,
                currency: 'usd',
                metadata: { test: true, source: 'test-endpoint' },
            });
            logStep('TEST', `Stripe intent created: ${intent.id}`);
            return intent;
        });

        logInfo('TEST', '✅ Stripe API test successful', { paymentIntentId: result.id });
        return res.json({
            success: true,
            message: 'Stripe API is working',
            paymentIntentId: result.id,
            clientSecret: result.client_secret?.substring(0, 20) + '...',
        });
    } catch (err) {
        logError('TEST', '❌ Stripe API test failed', err);
        return res.status(500).json({ success: false, message: 'Stripe API test failed', error: err.message });
    }
};

const testDatabase = async (req, res) => {
    console.log('[TEST] testDatabase called at', new Date().toISOString());
    logSeparator();
    logInfo('TEST', '🗄️ Database connection test started');
    logStep('TEST', 'Checking MongoDB connection');

    try {
        const mongoose = require('mongoose');
        const state = mongoose.connection.readyState;
        const states = { 0: 'disconnected', 1: 'connected', 2: 'connecting', 3: 'disconnecting' };
        console.log(`[TEST] MongoDB connection state: ${states[state]} (${state})`);

        logStep('TEST', `Connection state: ${states[state]} (${state})`);

        if (state !== 1) {
            return res.json({
                success: false,
                connectionState: states[state],
                readyState: state,
                message: 'Database not connected',
            });
        }

        const db = mongoose.connection.db;
        const pingResult = await db.admin().ping();
        console.log(`[TEST] Database ping result: ${pingResult.ok === 1 ? 'SUCCESS' : 'FAILED'}`);
        logStep('TEST', `Ping result: ${pingResult.ok === 1 ? 'SUCCESS' : 'FAILED'}`);

        return res.json({
            success: true,
            connectionState: states[state],
            readyState: state,
            databaseName: db.databaseName,
            ping: pingResult.ok === 1,
            message: 'Database connected successfully',
        });
    } catch (err) {
        console.error(`[TEST] Database test failed: ${err.message}`);
        logError('TEST', '❌ Database test failed', err);
        return res.status(500).json({ success: false, message: 'Database test failed', error: err.message });
    }
};

const testOrderBridge = async (req, res) => {
    console.log('[TEST] testOrderBridge called at', new Date().toISOString());
    logSeparator();
    logInfo('TEST', '🔗 Order Bridge test started');
    logStep('TEST', 'Testing order bridge service');

    try {
        const health = await orderBridgeService.getBridgeMetrics();
        console.log(`[TEST] Order bridge metrics retrieved:`, JSON.stringify(health));
        logStep('TEST', 'Order bridge metrics retrieved', health);
        return res.json({ success: true, message: 'Order bridge is reachable', metrics: health });
    } catch (err) {
        console.error(`[TEST] Order bridge test failed: ${err.message}`);
        logError('TEST', '❌ Order bridge test failed', err);
        return res.status(500).json({ success: false, message: 'Order bridge test failed', error: err.message });
    }
};

// ============================================================
// 💳 CREATE PAYMENT INTENT (Main Endpoint with GUARDIAN)
// ============================================================

const createPaymentIntent = async (req, res) => {
    const startTime = Date.now();
    console.log(`[PAYMENT] ========== CREATE PAYMENT INTENT STARTED ==========`);
    console.log(`[PAYMENT] Request ID: ${req.correlationId || 'N/A'}`);
    console.log(`[PAYMENT] Timestamp: ${new Date().toISOString()}`);
    console.log(`[PAYMENT] Headers:`, JSON.stringify({
        'content-type': req.headers['content-type'],
        'authorization': req.headers['authorization'] ? 'present' : 'missing',
        'idempotency-key': req.headers['idempotency-key'] ? 'present' : 'missing'
    }));

    resetStepCounter();
    logSeparator();
    logInfo('PAYMENT', '💰 CREATE PAYMENT INTENT STARTED');
    logStep('PAYMENT', 'Function entered', {
        hasBody: !!req.body,
        hasAuth: !!req.headers.authorization,
        user: req.user?.id || 'anonymous'
    });

    const start = Date.now();
    await ensureDBConnection();
    console.log(`[PAYMENT] DB connection ensured`);

    try {
        logStep('PAYMENT', 'STEP 1: Parsing request body');
        const { orderId, amount, currency = 'usd', items } = req.body;
        console.log(`[PAYMENT] Request body parsed: orderId=${orderId}, amount=${amount}, currency=${currency}, items=${items?.length || 0}`);
        logStep('PAYMENT', `STEP 2: Body parsed`, { orderId, amount, currency });

        logStep('PAYMENT', 'STEP 3: Getting userId');
        const userId = req.user?.id || `guest_${crypto.randomUUID().substring(0, 8)}`;
        console.log(`[PAYMENT] userId: ${userId}`);
        logStep('PAYMENT', `STEP 4: userId = ${userId}`);

        if (!orderId || !amount) {
            console.log(`[PAYMENT] Validation FAILED: missing orderId or amount`);
            logStep('PAYMENT', 'STEP 5: ❌ Validation FAILED - missing fields');
            return res.status(HTTP_STATUS.BAD_REQUEST).json({
                success: false,
                message: 'orderId and amount required',
                code: ERROR_CODES.MISSING_REQUIRED_FIELD,
            });
        }
        console.log(`[PAYMENT] Validation PASSED`);
        logStep('PAYMENT', 'STEP 5: ✅ Validation PASSED');

        logStep('PAYMENT', 'STEP 6: 🛡️ GUARDIAN - Validating order');
        const authToken = getAuthToken(req);
        console.log(`[PAYMENT] Calling guardianValidator.validateOrder for orderId=${orderId}`);
        const orderValidation = await guardianValidator.validateOrder(orderId, authToken);
        console.log(`[PAYMENT] Order validation result: valid=${orderValidation.valid}, message=${orderValidation.message}`);

        if (!orderValidation.valid) {
            console.log(`[PAYMENT] Order validation FAILED: ${orderValidation.message}`);
            logStep('PAYMENT', 'STEP 6a: ❌ Order validation failed', { message: orderValidation.message });
            return res.status(HTTP_STATUS.BAD_REQUEST).json({
                success: false,
                message: orderValidation.message,
                code: ERROR_CODES.ORDER_NOT_FOUND,
            });
        }
        console.log(`[PAYMENT] Order validation PASSED`);
        logStep('PAYMENT', 'STEP 6: ✅ Order validated');

        logStep('PAYMENT', 'STEP 7: 🛡️ GUARDIAN - Validating user risk');
        console.log(`[PAYMENT] Calling guardianValidator.validateUser for userId=${userId}, amount=${amount}`);
        const userValidation = await guardianValidator.validateUser(userId, authToken, amount);
        console.log(`[PAYMENT] User validation result: valid=${userValidation.valid}, riskScore=${userValidation.riskScore}, riskLevel=${userValidation.riskLevel}`);

        if (!userValidation.valid) {
            console.log(`[PAYMENT] User validation FAILED: riskScore=${userValidation.riskScore}`);
            logStep('PAYMENT', 'STEP 7a: ❌ User validation failed', { riskScore: userValidation.riskScore });
            return res.status(HTTP_STATUS.FORBIDDEN).json({
                success: false,
                message: 'Payment blocked due to security risk',
                code: ERROR_CODES.FRAUD_DETECTED,
                riskScore: userValidation.riskScore,
                recommendation: userValidation.recommendation,
            });
        }

        if (userValidation.requires2FA) {
            console.log(`[PAYMENT] 2FA required: riskScore=${userValidation.riskScore}`);
            logStep('PAYMENT', 'STEP 7b: ⚠️ 2FA required', { riskScore: userValidation.riskScore });
            return res.status(HTTP_STATUS.UNAUTHORIZED).json({
                success: false,
                message: 'Two-factor authentication required',
                code: '2FA_REQUIRED',
                riskScore: userValidation.riskScore,
            });
        }
        console.log(`[PAYMENT] User validation PASSED: riskScore=${userValidation.riskScore}`);
        logStep('PAYMENT', 'STEP 7: ✅ User validated', { riskScore: userValidation.riskScore });

        if (items && items.length > 0) {
            logStep('PAYMENT', 'STEP 8: 🛡️ GUARDIAN - Validating stock availability');
            console.log(`[PAYMENT] Validating stock for ${items.length} items`);
            const stockValidation = await guardianValidator.validateStock(orderId, items);
            console.log(`[PAYMENT] Stock validation result: valid=${stockValidation.valid}, message=${stockValidation.message}`);

            if (!stockValidation.valid) {
                console.log(`[PAYMENT] Stock validation FAILED: ${stockValidation.message}`);
                logStep('PAYMENT', 'STEP 8a: ❌ Stock validation failed', { message: stockValidation.message });
                return res.status(HTTP_STATUS.CONFLICT).json({
                    success: false,
                    message: stockValidation.message,
                    code: ERROR_CODES.INSUFFICIENT_STOCK,
                    reservations: stockValidation.reservations,
                });
            }
            console.log(`[PAYMENT] Stock validation PASSED: ${stockValidation.reservations.length} reservations created`);
            logStep('PAYMENT', 'STEP 8: ✅ Stock validated', { reservedCount: stockValidation.reservations.length });
        }

        logStep('PAYMENT', 'STEP 9: Generating idempotency key');
        const idempotencyKey = ZIC.generateKey(userId, orderId);
        console.log(`[PAYMENT] Idempotency key: ${idempotencyKey.substring(0, 16)}... (full length: ${idempotencyKey.length})`);
        logStep('PAYMENT', `STEP 10: Idempotency key generated: ${idempotencyKey.substring(0, 16)}...`);

        logStep('PAYMENT', 'STEP 11: Acquiring lock from ZIC');
        const lock = ZIC.acquire(idempotencyKey);
        console.log(`[PAYMENT] Lock result: ${lock.allowed ? 'ACQUIRED' : 'BLOCKED'}`);
        logStep('PAYMENT', `STEP 12: Lock result: ${lock.allowed ? 'ACQUIRED' : 'BLOCKED'}`);

        if (!lock.allowed) {
            console.log(`[PAYMENT] Duplicate request blocked`);
            logStep('PAYMENT', 'STEP 13: ❌ Duplicate request - returning 409');
            return res.status(HTTP_STATUS.CONFLICT).json({
                success: false,
                message: 'Duplicate payment request blocked',
                code: lock.reason,
            });
        }

        logStep('PAYMENT', 'STEP 14: Looking up payment record in DB');
        let payment = await Payment.findOne({ orderId });
        console.log(`[PAYMENT] Payment lookup result: ${payment ? `FOUND (${payment._id})` : 'NOT FOUND'}`);
        logStep('PAYMENT', `STEP 15: Payment lookup result: ${payment ? `FOUND (${payment._id})` : 'NOT FOUND'}`);

        if (!payment) {
            logStep('PAYMENT', 'STEP 16: Creating new payment record');
            console.log(`[PAYMENT] Creating new payment record with anomaly detection`);
            payment = await Payment.createWithAnomalyDetection({
                orderId, userId, amount, currency,
                status: PAYMENT_STATUS.CREATED,
                idempotencyKey,
                riskLevel: userValidation.riskLevel,
                anomalyScore: userValidation.riskScore,
            });
            console.log(`[PAYMENT] Payment created: ${payment._id}`);
            logStep('PAYMENT', `STEP 17: ✅ Payment created: ${payment._id}`);
        } else {
            console.log(`[PAYMENT] Existing payment found, skipping creation`);
        }

        // ============================================================
        // 🧠 FIX: Store reservation ID for BOTH new and existing payments
        // ============================================================
        console.log(`[PAYMENT] Checking for reservation ID in order...`);
        console.log(`[PAYMENT] orderValidation.order?.reservations:`, JSON.stringify(orderValidation.order?.reservations));

        if (orderValidation.order?.reservations && orderValidation.order.reservations.length > 0) {
            const reservationId = orderValidation.order.reservations[0].reservationId;
            console.log(`[PAYMENT] Found reservation ID from order: ${reservationId}`);

            if (!payment.reservationId) {
                payment.reservationId = reservationId;
                await payment.save();
                console.log(`[PAYMENT] ✅ Reservation ID stored in payment: ${payment.reservationId}`);
                logStep('PAYMENT', `Reservation ID stored: ${payment.reservationId}`);
            } else if (payment.reservationId !== reservationId) {
                console.log(`[PAYMENT] Updating reservation ID from ${payment.reservationId} to ${reservationId}`);
                payment.reservationId = reservationId;
                await payment.save();
                console.log(`[PAYMENT] ✅ Reservation ID updated in payment`);
                logStep('PAYMENT', `Reservation ID updated: ${payment.reservationId}`);
            } else {
                console.log(`[PAYMENT] Reservation ID already exists: ${payment.reservationId}`);
            }
        } else {
            console.log(`[PAYMENT] ⚠️ No reservation ID found in order - this may cause issues with inventory management`);
        }
        // ============================================================

        logStep('PAYMENT', 'STEP 18: Creating transaction via SENTINEL');
        console.log(`[PAYMENT] Creating transaction for order ${orderId}, payment ${payment._id}, amount ${amount}`);
        const transaction = await sentinelCoordinator.createTransaction(orderId, payment._id, userId, amount);
        console.log(`[PAYMENT] Transaction created: ${transaction.id.substring(0, 8)}...`);
        logStep('PAYMENT', `STEP 19: ✅ Transaction created: ${transaction.id.substring(0, 8)}`);

        logStep('PAYMENT', 'STEP 20: Calling Stripe API via requestCoalescer');
        console.log(`[PAYMENT] Calling Stripe API to create payment intent for amount ${amount} ${currency}`);

        const stripeIntent = await requestCoalescer.coalesce(userId, orderId, amount, async () => {
            console.log(`[PAYMENT] Inside coalescer callback - calling safeStripeExecute`);
            logStep('PAYMENT', 'STEP 20b: Inside coalescer callback - calling safeStripeExecute');
            return await safeStripeExecute(async (stripe) => {
                console.log(`[PAYMENT] Inside safeStripeExecute - calling stripe.paymentIntents.create`);
                logStep('PAYMENT', 'STEP 20c: Inside safeStripeExecute - calling stripe.paymentIntents.create');
                logStep('PAYMENT', `STEP 20d: Amount in cents: ${Math.round(amount * 100)}`);

                const {
                    returnUrl = null,
                    paymentMethodTypes = ['card'],
                    paymentMethodId = null,
                    confirmManually = false,
                    forceCardOnly = process.env.FORCE_CARD_ONLY === 'true' || false
                } = req.body;

                console.log(`[PAYMENT] Payment intent options: forceCardOnly=${forceCardOnly}, isRedirectPayment=${!!returnUrl}, paymentMethodTypes=${JSON.stringify(paymentMethodTypes)}`);

                const isRedirectPayment = !forceCardOnly && returnUrl && paymentMethodTypes.some(type =>
                    ['klarna', 'affirm', 'afterpay', 'cashapp', 'amazon_pay'].includes(type)
                );

                const paymentIntentOptions = {
                    amount: Math.round(amount * 100),
                    currency,
                    metadata: {
                        orderId,
                        userId,
                        idempotencyKey,
                        transactionId: transaction.id,
                        riskScore: userValidation.riskScore,
                    },
                };

                if (forceCardOnly) {
                    console.log(`[PAYMENT] FORCE CARD-ONLY MODE ACTIVE`);
                    logStep('PAYMENT', '🚀 FORCE CARD-ONLY MODE ACTIVE');
                    paymentIntentOptions.payment_method_types = ['card'];
                    paymentIntentOptions.automatic_payment_methods = { enabled: false };
                    paymentIntentOptions.confirm = true;
                    paymentIntentOptions.payment_method = paymentMethodId || 'pm_card_visa';
                    console.log(`[PAYMENT] Auto-confirming payment with test card`);
                    logStep('PAYMENT', 'Auto-confirming payment with test card');
                } else if (isRedirectPayment || returnUrl) {
                    console.log(`[PAYMENT] Using automatic payment methods with redirect`);
                    logStep('PAYMENT', 'Using automatic payment methods with redirect');
                    paymentIntentOptions.automatic_payment_methods = { enabled: true, allow_redirects: 'always' };
                    paymentIntentOptions.return_url = returnUrl || 'http://localhost:5004/payment-complete';
                } else {
                    console.log(`[PAYMENT] Using card-only payment methods: ${JSON.stringify(paymentMethodTypes)}`);
                    logStep('PAYMENT', 'Using card-only payment methods');
                    paymentIntentOptions.payment_method_types = paymentMethodTypes;
                    paymentIntentOptions.automatic_payment_methods = { enabled: false };

                    if (paymentMethodId && !confirmManually) {
                        paymentIntentOptions.confirm = true;
                        paymentIntentOptions.payment_method = paymentMethodId;
                        console.log(`[PAYMENT] Auto-confirming payment with provided payment method`);
                        logStep('PAYMENT', 'Auto-confirming payment with provided payment method');
                    } else if (!confirmManually && process.env.NODE_ENV === 'development') {
                        paymentIntentOptions.confirm = true;
                        paymentIntentOptions.payment_method = 'pm_card_visa';
                        console.log(`[PAYMENT] Using test card for auto-confirmation`);
                        logStep('PAYMENT', 'Using test card for auto-confirmation');
                    }
                }

                const result = await stripe.paymentIntents.create(paymentIntentOptions);
                console.log(`[PAYMENT] ✅ Stripe API returned! Intent ID: ${result.id}, status: ${result.status}, client_secret: ${result.client_secret ? 'present' : 'missing'}`);
                logStep('PAYMENT', `STEP 20e: ✅ Stripe API returned! Intent ID: ${result.id}`);
                logStep('PAYMENT', `Payment status: ${result.status}`);
                return result;
            }, { idempotencyKey });
        });

        console.log(`[PAYMENT] ✅ Stripe intent created: ${stripeIntent.id}, client_secret: ${stripeIntent.client_secret ? 'present' : 'missing'}`);
        logStep('PAYMENT', `STEP 21: ✅ Stripe intent created: ${stripeIntent.id}`);

        logStep('PAYMENT', 'STEP 22: Building ledger node');
        const ledgerNode = buildLedgerNode(null, {
            type: 'PAYMENT_INTENT_CREATED',
            orderId,
            paymentIntentId: stripeIntent.id,
            amount,
            transactionId: transaction.id,
            riskScore: userValidation.riskScore,
        });

        logStep('PAYMENT', 'STEP 23: Updating payment record');
        if (!payment.ledger) {
            payment.ledger = [];
        }
        payment.ledger.push(ledgerNode);
        payment.status = PAYMENT_STATUS.PENDING;
        payment.stripePaymentIntentId = stripeIntent.id;
        await payment.save();
        console.log(`[PAYMENT] Payment record updated with Stripe intent ID`);
        logStep('PAYMENT', 'STEP 24: ✅ Payment record updated');

        logStep('PAYMENT', 'STEP 25: Releasing idempotency lock');
        ZIC.release(idempotencyKey, 'PENDING');

        const processingTime = Date.now() - start;
        console.log(`[PAYMENT] ✅ SUCCESS! Total processing time: ${processingTime}ms`);
        logStep('PAYMENT', `STEP 26: 🎉 SUCCESS! Total time: ${processingTime}ms`);
        logSeparator();

        return res.status(HTTP_STATUS.CREATED).json({
            success: true,
            clientSecret: stripeIntent.client_secret,
            paymentId: payment._id,
            transactionId: transaction.id,
            processingTimeMs: processingTime,
            riskScore: userValidation.riskScore,
        });

    } catch (err) {
        const processingTime = Date.now() - start;
        console.error(`[PAYMENT] ❌ CRITICAL ERROR at step ${stepCounter}:`, err);
        console.error(`[PAYMENT] Error stack:`, err.stack);
        logError('PAYMENT', `❌ CRITICAL ERROR at step ${stepCounter}`, err);

        // ============================================================
        // 🧠 FIX: Release reservation if payment creation fails
        // ============================================================
        console.log(`[PAYMENT] Attempting to release reservation on error`);
        try {
            const { orderId } = req.body;
            if (orderId) {
                const authToken = getAuthToken(req);
                const orderServiceClient = require('../integrations/orderServiceClient');
                console.log(`[PAYMENT] Fetching order to get reservation ID: ${orderId}`);
                const order = await orderServiceClient.getOrder(orderId, authToken);
                console.log(`[PAYMENT] Order retrieved, reservations: ${order?.reservations?.length || 0}`);

                const reservationId = order?.reservations?.[0]?.reservationId;
                if (reservationId) {
                    const productServiceClient = require('../integrations/productServiceClient');
                    console.log(`[PAYMENT] Releasing reservation: ${reservationId}`);
                    await productServiceClient.releaseReservation(reservationId);
                    console.log(`[PAYMENT] ✅ Reservation released on error: ${reservationId}`);
                    logStep('PAYMENT', `✅ Reservation released on error: ${reservationId}`);
                } else {
                    console.log(`[PAYMENT] ⚠️ No reservation ID found to release`);
                    logStep('PAYMENT', '⚠️ No reservation ID found to release');
                }
            }
        } catch (releaseErr) {
            console.error(`[PAYMENT] Failed to release reservation on error: ${releaseErr.message}`);
            logError('PAYMENT', 'Failed to release reservation on error', releaseErr);
            // Don't re-throw - we already have an error to return
        }
        // ============================================================

        logSeparator();

        return res.status(HTTP_STATUS.INTERNAL_SERVER_ERROR).json({
            success: false,
            message: 'Payment initiation failed',
            code: err.message?.includes('circuit') ? ERROR_CODES.CIRCUIT_OPEN : ERROR_CODES.INTERNAL_SERVER_ERROR,
            processingTimeMs: processingTime,
            error: err.message,
            stack: err.stack,
        });
    }
};
// ============================================================
// 💰 CONFIRM PAYMENT
// ============================================================

const confirmPayment = async (req, res) => {
    const startTime = Date.now();
    console.log(`[PAYMENT] ========== CONFIRM PAYMENT STARTED ==========`);
    console.log(`[PAYMENT] Request ID: ${req.correlationId || 'N/A'}`);
    console.log(`[PAYMENT] Timestamp: ${new Date().toISOString()}`);
    console.log(`[PAYMENT] Headers:`, JSON.stringify({
        'content-type': req.headers['content-type'],
        'authorization': req.headers['authorization'] ? 'present' : 'missing',
        'idempotency-key': req.headers['idempotency-key'] ? 'present' : 'missing'
    }));

    resetStepCounter();
    logSeparator();
    logInfo('PAYMENT', '💰 CONFIRM PAYMENT STARTED');

    const start = Date.now();
    await ensureDBConnection();
    console.log(`[PAYMENT] DB connection ensured`);

    const authToken = getAuthToken(req);
    console.log(`[PAYMENT] Auth token present: ${!!authToken}`);
    logStep('PAYMENT', `Auth token present: ${!!authToken}`);

    try {
        const { paymentIntentId } = req.body;
        console.log(`[PAYMENT] PaymentIntentId: ${paymentIntentId}`);
        logStep('PAYMENT', `PaymentIntentId: ${paymentIntentId}`);

        if (!paymentIntentId) {
            console.log(`[PAYMENT] Missing paymentIntentId`);
            logStep('PAYMENT', '❌ Missing paymentIntentId');
            return res.status(HTTP_STATUS.BAD_REQUEST).json({
                success: false,
                message: 'paymentIntentId required',
                code: ERROR_CODES.MISSING_REQUIRED_FIELD,
            });
        }

        logStep('PAYMENT', 'Looking up payment record');
        console.log(`[PAYMENT] Looking up payment by stripePaymentIntentId: ${paymentIntentId}`);
        const payment = await Payment.findOne({ stripePaymentIntentId: paymentIntentId });

        if (!payment) {
            console.log(`[PAYMENT] Payment not found for intent: ${paymentIntentId}`);
            logStep('PAYMENT', '❌ Payment not found');
            return res.status(HTTP_STATUS.NOT_FOUND).json({
                success: false,
                message: 'Payment not found',
                code: ERROR_CODES.PAYMENT_NOT_FOUND,
            });
        }
        console.log(`[PAYMENT] ✅ Payment found: ${payment._id}, status: ${payment.status}, hasReservationId: ${!!payment.reservationId}`);
        logStep('PAYMENT', `✅ Payment found: ${payment._id}, status: ${payment.status}`);

        logStep('PAYMENT', 'Looking up transaction');
        const transaction = await Transaction.findOne({ paymentId: payment._id });
        console.log(`[PAYMENT] Transaction found: ${!!transaction}`);
        logStep('PAYMENT', `Transaction found: ${!!transaction}`);

        logStep('PAYMENT', 'Executing with hedge for Stripe confirmation');
        console.log(`[PAYMENT] Executing Stripe confirmation with hedge`);
        const confirmResult = await shadowExecutor.executeWithHedge(
            async () => {
                console.log(`[PAYMENT] Primary confirmation attempt`);
                logStep('PAYMENT', 'Primary confirmation attempt');
                return await safeStripeExecute(async (stripe) => {
                    return stripe.paymentIntents.retrieve(paymentIntentId);
                });
            },
            async () => {
                console.log(`[PAYMENT] Hedge confirmation attempt`);
                logStep('PAYMENT', 'Hedge confirmation attempt');
                return await safeStripeExecute(async (stripe) => {
                    return stripe.paymentIntents.retrieve(paymentIntentId, { timeout: 3000 });
                });
            }
        );

        const result = confirmResult.result;
        console.log(`[PAYMENT] Stripe status: ${result.status}, Hedged: ${confirmResult.hedged}`);
        logStep('PAYMENT', `Stripe status: ${result.status}, Hedged: ${confirmResult.hedged}`);

        if (result.status !== 'succeeded') {
            console.log(`[PAYMENT] Payment not completed, status: ${result.status}`);
            logStep('PAYMENT', '❌ Payment not completed');
            return res.status(HTTP_STATUS.BAD_REQUEST).json({
                success: false,
                message: 'Payment not completed',
                code: ERROR_CODES.PAYMENT_CONFIRMATION_FAILED,
            });
        }

        logStep('PAYMENT', 'Updating ledger and payment status');
        const lastNode = payment.ledger && payment.ledger.length > 0 ? payment.ledger[payment.ledger.length - 1] : null;
        payment.ledger.push(buildLedgerNode(
            lastNode,
            { type: 'PAYMENT_CONFIRMED', paymentIntentId, hedged: confirmResult.hedged }
        ));

        payment.status = PAYMENT_STATUS.SUCCEEDED;
        payment.paidAt = new Date();
        await payment.save();
        console.log(`[PAYMENT] ✅ Payment status updated to SUCCEEDED`);
        logStep('PAYMENT', '✅ Payment status updated to SUCCEEDED');

        logStep('PAYMENT', 'Completing transaction via SENTINEL');
        const txnId = transaction?.idempotencyKey?.replace('txn_', '') || transaction?._id?.toString();
        await sentinelCoordinator.completeTransaction(txnId, result);
        console.log(`[PAYMENT] Transaction completed`);

        // ============================================================
        // 🧠 FIX: Release reservation after successful payment (FIRE AND FORGET)
        // ============================================================
        if (payment.reservationId) {
            console.log(`[PAYMENT] Releasing reservation (fire and forget): ${payment.reservationId}`);
            // Fire and forget - don't block the response
            setTimeout(async () => {
                try {
                    const productServiceClient = require('../integrations/productServiceClient');
                    await productServiceClient.releaseReservation(payment.reservationId);
                    console.log(`[PAYMENT] ✅ Reservation released: ${payment.reservationId}`);
                } catch (err) {
                    console.error(`[PAYMENT] Failed to release reservation: ${err.message}`);
                }
            }, 100);
        } else {
            console.log(`[PAYMENT] ⚠️ No reservation ID to release for payment ${payment._id}`);
        }
        // ============================================================

        logStep('PAYMENT', 'Notifying Order Service with auth token');
        console.log(`[PAYMENT] Notifying Order Service about successful payment for order ${payment.orderId}`);
        await orderBridgeService.confirmOrderAfterPayment({
            orderId: payment.orderId,
            paymentIntentId,
            userId: payment.userId,
            authToken,
        });
        console.log(`[PAYMENT] Order Service notified`);

        const processingTime = Date.now() - start;
        console.log(`[PAYMENT] 🎉 Payment confirmed successfully in ${processingTime}ms`);
        logStep('PAYMENT', `🎉 Payment confirmed successfully in ${processingTime}ms`);
        logSeparator();

        return res.json({
            success: true,
            message: 'Payment confirmed',
            orderId: payment.orderId,
            hedged: confirmResult.hedged,
            processingTimeMs: processingTime,
        });

    } catch (err) {
        const processingTime = Date.now() - start;
        console.error(`[PAYMENT] ❌ confirmPayment failed: ${err.message}`);
        console.error(`[PAYMENT] Error stack:`, err.stack);
        logError('PAYMENT', '❌ confirmPayment failed', err);
        logSeparator();

        return res.status(HTTP_STATUS.INTERNAL_SERVER_ERROR).json({
            success: false,
            message: 'Payment confirmation failed',
            code: ERROR_CODES.PAYMENT_CONFIRMATION_FAILED,
            processingTimeMs: processingTime,
        });
    }
};
// ============================================================
// ❌ CANCEL PAYMENT
// ============================================================

const cancelPayment = async (req, res) => {
    const startTime = Date.now();
    console.log(`[PAYMENT] ========== CANCEL PAYMENT STARTED ==========`);
    console.log(`[PAYMENT] Request ID: ${req.correlationId || 'N/A'}`);
    console.log(`[PAYMENT] Timestamp: ${new Date().toISOString()}`);

    resetStepCounter();
    logSeparator();
    logInfo('PAYMENT', '❌ CANCEL PAYMENT STARTED');

    const start = Date.now();
    await ensureDBConnection();

    const authToken = getAuthToken(req);
    console.log(`[PAYMENT] Auth token present: ${!!authToken}`);

    try {
        const { paymentIntentId } = req.body;
        console.log(`[PAYMENT] PaymentIntentId: ${paymentIntentId}`);
        logStep('PAYMENT', `PaymentIntentId: ${paymentIntentId}`);

        if (!paymentIntentId) {
            console.log(`[PAYMENT] Missing paymentIntentId`);
            return res.status(HTTP_STATUS.BAD_REQUEST).json({
                success: false,
                message: 'paymentIntentId required',
                code: ERROR_CODES.MISSING_REQUIRED_FIELD,
            });
        }

        console.log(`[PAYMENT] Looking up payment record for intent: ${paymentIntentId}`);
        const payment = await Payment.findOne({ stripePaymentIntentId: paymentIntentId });
        if (!payment) {
            console.log(`[PAYMENT] Payment not found for intent: ${paymentIntentId}`);
            return res.status(HTTP_STATUS.NOT_FOUND).json({
                success: false,
                message: 'Payment not found',
                code: ERROR_CODES.PAYMENT_NOT_FOUND,
            });
        }
        console.log(`[PAYMENT] ✅ Payment found: ${payment._id}`);
        logStep('PAYMENT', `✅ Payment found: ${payment._id}`);

        logStep('PAYMENT', 'Cancelling Stripe payment intent');
        console.log(`[PAYMENT] Cancelling Stripe payment intent: ${paymentIntentId}`);
        await safeStripeExecute(async (stripe) => {
            return stripe.paymentIntents.cancel(paymentIntentId);
        });
        console.log(`[PAYMENT] Stripe payment intent cancelled`);

        payment.status = PAYMENT_STATUS.CANCELLED;
        payment.cancelledAt = new Date();
        const lastNode = payment.ledger && payment.ledger.length > 0 ? payment.ledger[payment.ledger.length - 1] : null;
        payment.ledger.push(buildLedgerNode(lastNode, { type: 'PAYMENT_CANCELLED' }));
        await payment.save();
        console.log(`[PAYMENT] ✅ Payment cancelled in database`);
        logStep('PAYMENT', '✅ Payment cancelled');

        // ============================================================
        // 🧠 FIX: Release reservation when payment is cancelled
        // ============================================================
        console.log(`[PAYMENT] Releasing product reservation due to cancellation`);
        logStep('PAYMENT', 'Releasing product reservation due to cancellation');

        let reservationReleased = false;

        // First try using stored reservation ID from payment record
        if (payment.reservationId) {
            console.log(`[PAYMENT] Found stored reservation ID in payment record: ${payment.reservationId}`);
            try {
                const productServiceClient = require('../integrations/productServiceClient');
                await productServiceClient.releaseReservation(payment.reservationId);
                console.log(`[PAYMENT] ✅ Reservation released from stored ID on cancellation: ${payment.reservationId}`);
                logStep('PAYMENT', `✅ Reservation released on cancellation: ${payment.reservationId}`);
                reservationReleased = true;
            } catch (err) {
                console.error(`[PAYMENT] Failed to release reservation from stored ID: ${err.message}`);
                logError('PAYMENT', 'Failed to release reservation on cancel', err);
            }
        }

        // If stored ID didn't work, try fetching from order
        if (!reservationReleased) {
            console.log(`[PAYMENT] Fetching order to get reservation ID`);
            try {
                const orderServiceClient = require('../integrations/orderServiceClient');
                const order = await orderServiceClient.getOrder(payment.orderId, authToken);
                console.log(`[PAYMENT] Order retrieved, reservations: ${order?.reservations?.length || 0}`);

                const reservationId = order?.reservations?.[0]?.reservationId;
                if (reservationId) {
                    const productServiceClient = require('../integrations/productServiceClient');
                    await productServiceClient.releaseReservation(reservationId);
                    console.log(`[PAYMENT] ✅ Reservation released from order on cancellation: ${reservationId}`);
                    logStep('PAYMENT', `✅ Reservation released on cancellation: ${reservationId}`);
                    reservationReleased = true;
                } else {
                    console.log(`[PAYMENT] ⚠️ No reservation ID found in order`);
                    logStep('PAYMENT', '⚠️ No reservation ID found in order');
                }
            } catch (err) {
                console.error(`[PAYMENT] Failed to release reservation from order: ${err.message}`);
                logError('PAYMENT', 'Failed to release reservation on cancel', err);
            }
        }

        if (!reservationReleased) {
            console.log(`[PAYMENT] ⚠️ CRITICAL: Reservation could not be released on cancellation`);
        }
        // ============================================================

        const transaction = await Transaction.findOne({ paymentId: payment._id });
        if (transaction) {
            const txnId = transaction.idempotencyKey?.replace('txn_', '') || transaction._id?.toString();
            console.log(`[PAYMENT] Failing transaction: ${txnId}`);
            await sentinelCoordinator.failTransaction(txnId, new Error('Payment cancelled'));
        }

        console.log(`[PAYMENT] Notifying Order Service about cancellation`);
        await orderBridgeService.cancelOrderAfterPaymentFailure({
            orderId: payment.orderId,
            reason: 'payment_cancelled',
            authToken,
        });
        console.log(`[PAYMENT] Order Service notified`);

        const processingTime = Date.now() - start;
        console.log(`[PAYMENT] ✅ Payment cancelled successfully in ${processingTime}ms`);
        logStep('PAYMENT', `✅ Payment cancelled successfully in ${processingTime}ms`);
        logSeparator();

        return res.json({
            success: true,
            message: 'Payment cancelled',
            processingTimeMs: processingTime,
        });

    } catch (err) {
        console.error(`[PAYMENT] ❌ cancelPayment failed: ${err.message}`);
        logError('PAYMENT', '❌ cancelPayment failed', err);
        return res.status(HTTP_STATUS.INTERNAL_SERVER_ERROR).json({
            success: false,
            message: 'Payment cancellation failed',
            code: ERROR_CODES.PAYMENT_CANCELLATION_FAILED,
        });
    }
};

// ============================================================
// 📊 PAYMENT STATUS
// ============================================================

const getPaymentStatus = async (req, res) => {
    // Accept both orderId and paymentId parameter names
    const orderId = req.params.orderId || req.params.paymentId;
    console.log(`[PAYMENT] getPaymentStatus called for orderId: ${orderId}, timestamp=${new Date().toISOString()}`);
    logStep('PAYMENT', '📊 getPaymentStatus called', { orderId });

    try {
        if (!orderId) {
            console.log(`[PAYMENT] Missing orderId`);
            return res.status(HTTP_STATUS.BAD_REQUEST).json({
                success: false,
                message: 'orderId required',
                code: ERROR_CODES.MISSING_REQUIRED_FIELD,
            });
        }

        console.log(`[PAYMENT] Looking up payment by orderId: ${orderId}`);
        const payment = await Payment.findOne({ orderId });

        if (!payment) {
            console.log(`[PAYMENT] Payment not found for orderId: ${orderId}`);
            return res.status(HTTP_STATUS.NOT_FOUND).json({
                success: false,
                message: 'Payment not found',
                code: ERROR_CODES.PAYMENT_NOT_FOUND,
            });
        }

        console.log(`[PAYMENT] Payment found: status=${payment.status}, amount=${payment.amount}, hasReservationId=${!!payment.reservationId}`);
        return res.json({
            success: true,
            payment: {
                orderId: payment.orderId,
                status: payment.status,
                amount: payment.amount,
                paidAt: payment.paidAt,
                ledgerDepth: payment.ledger?.length || 0,
                anomalyScore: payment.anomalyScore || 0,
                riskLevel: payment.riskLevel || RISK_LEVELS.NORMAL,
                reservationId: payment.reservationId || null,  // ✅ ADDED THIS LINE
            },
        });

    } catch (err) {
        console.error(`[PAYMENT] getPaymentStatus failed: ${err.message}`);
        logError('PAYMENT', 'getPaymentStatus failed', err);
        return res.status(HTTP_STATUS.INTERNAL_SERVER_ERROR).json({
            success: false,
            message: 'Status fetch failed',
            code: ERROR_CODES.INTERNAL_SERVER_ERROR,
        });
    }
};

// ============================================================
// 🧠 HEALTH CHECK ENDPOINT
// ============================================================

const getSystemHealth = async (req, res) => {
    console.log(`[PAYMENT] getSystemHealth called at ${new Date().toISOString()}`);
    logStep('PAYMENT', '🏥 Health check requested');
    const healthCheck = await phoenixBreaker.healthCheck();
    const isHealthy = Object.values(healthCheck).every(h => h.healthScore > 50);
    console.log(`[PAYMENT] System health: ${isHealthy ? 'HEALTHY' : 'DEGRADED'}`);

    return res.json({
        success: true,
        timestamp: new Date().toISOString(),
        status: isHealthy ? 'HEALTHY' : 'DEGRADED',
        circuits: phoenixBreaker.getMetrics(),
        coalescer: requestCoalescer.getStats(),
        shadow: shadowExecutor.getStats(),
        sentinel: sentinelCoordinator.getMetrics(),
        guardian: guardianValidator.getMetrics(),
        zicStoreSize: idempotencyStore.size,
    });
};

// ============================================================
// ✅ SINGLE EXPORT - COMBINED
// ============================================================

console.log('[PAYMENT-CONTROLLER] ========================================');
console.log('[PAYMENT-CONTROLLER] PAYMENT CONTROLLER EXPORTS:');
console.log('[PAYMENT-CONTROLLER]   - createPaymentIntent');
console.log('[PAYMENT-CONTROLLER]   - confirmPayment');
console.log('[PAYMENT-CONTROLLER]   - cancelPayment');
console.log('[PAYMENT-CONTROLLER]   - getPaymentStatus');
console.log('[PAYMENT-CONTROLLER]   - getSystemHealth');
console.log('[PAYMENT-CONTROLLER]   - testPing, testStripeDirect, testDatabase, testOrderBridge');
console.log('[PAYMENT-CONTROLLER]   - directStripePayment');
console.log('[PAYMENT-CONTROLLER]   - requestCoalescer, phoenixBreaker, shadowExecutor, sentinelCoordinator, guardianValidator');
console.log('[PAYMENT-CONTROLLER] ========================================');

module.exports = {
    // Main payment endpoints
    createPaymentIntent,
    confirmPayment,
    cancelPayment,
    getPaymentStatus,
    getSystemHealth,

    // Test endpoints (for debugging)
    testPing,
    testStripeDirect,
    testDatabase,
    testOrderBridge,

    // Direct Stripe test endpoint
    directStripePayment: async (req, res) => {
        console.log(`[TEST] directStripePayment called at ${new Date().toISOString()}`);
        try {
            const { amount, currency = 'usd' } = req.body;
            console.log(`[TEST] Creating direct Stripe payment intent: amount=${amount}, currency=${currency}`);
            const stripe = require('stripe')(process.env.STRIPE_SECRET_KEY);
            const paymentIntent = await stripe.paymentIntents.create({
                amount: Math.round(amount * 100),
                currency,
                metadata: { test: true, source: 'direct-test' }
            });
            console.log(`[TEST] Direct Stripe payment intent created: ${paymentIntent.id}`);
            res.json({
                success: true,
                clientSecret: paymentIntent.client_secret,
                paymentIntentId: paymentIntent.id
            });
        } catch (error) {
            console.error(`[TEST] Direct stripe error: ${error.message}`);
            res.json({ success: false, error: error.message });
        }
    },

    // Algorithm instances (for monitoring)
    requestCoalescer,
    phoenixBreaker,
    shadowExecutor,
    sentinelCoordinator,
    guardianValidator,
};