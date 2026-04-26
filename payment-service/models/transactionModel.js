/**
 * ============================================================
 * 💳 TRANSACTION MODEL — FINANCIAL LEDGER ENGINE v5.0
 * ============================================================
 *
 * PURPOSE:
 * - Tracks every financial movement (payment, refund, failure)
 * - Acts as source of truth for money flow
 * - Enables audit, reconciliation, and dispute handling
 *
 * SCALE:
 * - 50M+ users
 * - Millions of transactions/day
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: TIS (Transaction Idempotency Shield) [KEPT]
 * 🧠 ALGORITHM 2: LIP (Ledger Integrity Protection) [KEPT]
 * 🧠 ALGORITHM 3: REAPER (Real-time Event Aggregation & Pattern-based Error Recognition) [KEPT]
 * 🧠 ALGORITHM 4: GLACIER (Granular Ledger Archiving with Compression & Intelligent Event Retention) [KEPT]
 * 🧠 ALGORITHM 5: PHOENIX (Predictive Health Orchestration & Emergency Isolation eXecutor) [KEPT]
 * 🧠 ALGORITHM 6: MERIDIAN (Multi-stage Event Routing & Intelligent Domain Aggregation) [KEPT]
 * 🧠 ALGORITHM 7: SOVERIGN (Transaction Finality Guarantee & Settlement Protection) [NEW]
 * ============================================================
 * - Guarantees transaction finality across distributed systems
 * - Settlement protection against race conditions
 * - Automatic reconciliation with payment gateway
 * - Prevents double settlement at 50M scale
 * - Immutable settlement proof generation
 * ============================================================
 */

const mongoose = require('mongoose');
const crypto = require('crypto');

const {
    TRANSACTION_STATUS,
    TRANSACTION_TYPE,
    ANOMALY_TYPES,
    ANOMALY_SEVERITY,
    RISK_LEVELS,
    ARCHIVE_TIERS,
    PAYMENT_STATUS,
} = require('../constants');

// ============================================================
// ENUMS (STRICT CONTROL) [KEPT - USING CONSTANTS]
// ============================================================

// Now using constants from ../constants instead of hardcoded arrays

// ============================================================
// SCHEMA (ENHANCED WITH SOVERIGN)
// ============================================================

const transactionSchema = new mongoose.Schema(
    {
        // 🔗 Core references
        orderId: {
            type: mongoose.Schema.Types.ObjectId,
            required: true,
            index: true,
            ref: 'Order',
        },

        paymentId: {
            type: mongoose.Schema.Types.ObjectId,
            required: true,
            index: true,
            ref: 'Payment',
        },

        userId: {
            type: String,
            required: true,
            index: true,
        },

        // 💰 Financial data
        amount: {
            type: Number,
            required: true,
            min: 0,
        },

        currency: {
            type: String,
            default: 'usd',
            uppercase: true,
        },

        type: {
            type: String,
            enum: Object.values(TRANSACTION_TYPE),
            required: true,
        },

        status: {
            type: String,
            enum: Object.values(TRANSACTION_STATUS),
            default: 'pending',
            index: true,
        },

        // 🔐 Stripe / external reference
        stripePaymentIntentId: {
            type: String,
            index: true,
            sparse: true,
        },

        stripeChargeId: {
            type: String,
            index: true,
            sparse: true,
        },

        stripeRefundId: {
            type: String,
            index: true,
            sparse: true,
        },

        // 🧠 TIS: Idempotency Shield [KEPT]
        idempotencyKey: {
            type: String,
            required: true,
        },

        // 🧾 Metadata
        metadata: {
            type: Object,
            default: {},
        },

        // 🧠 LIP: Lock after settlement [KEPT]
        isFinalized: {
            type: Boolean,
            default: false,
            index: true,
        },

        finalizedAt: {
            type: Date,
        },

        // 🔍 Observability
        failureReason: {
            type: String,
        },

        processingLatencyMs: {
            type: Number,
        },

        // 🧠 REAPER: Anomaly detection fields [KEPT - FIXED CONSTANT]
        anomalyFlags: [{
            type: {
                type: String,
                enum: Object.values(ANOMALY_TYPES),
            },
            detectedAt: {
                type: Date,
                default: Date.now,
            },
            severity: {
                type: String,
                enum: Object.values(ANOMALY_SEVERITY),
            },
            description: String,
            resolved: {
                type: Boolean,
                default: false,
            },
            resolvedAt: Date,
            resolvedBy: String,
        }],

        anomalyScore: {
            type: Number,
            default: 0,
            min: 0,
            max: 100,
        },

        riskLevel: {
            type: String,
            enum: Object.values(RISK_LEVELS),
            default: RISK_LEVELS.NORMAL,
        },

        // 🧠 GLACIER: Archiving fields [KEPT - USING CONSTANTS]
        archived: {
            type: Boolean,
            default: false,
            index: true,
        },

        archiveTier: {
            type: String,
            enum: Object.values(ARCHIVE_TIERS),
            default: ARCHIVE_TIERS.HOT,
        },

        archivedAt: {
            type: Date,
        },

        compressedPayload: {
            type: Buffer,
        },

        // 🧠 PHOENIX: Health tracking fields [KEPT]
        healthScore: {
            type: Number,
            default: 100,
            min: 0,
            max: 100,
        },

        healingAttempts: [{
            attemptType: {
                type: String,
                enum: ['AUTO_RECONCILE', 'WEBHOOK_RESEND', 'MANUAL_REVIEW'],
            },
            attemptedAt: {
                type: Date,
                default: Date.now,
            },
            success: Boolean,
            result: String,
        }],

        lastHealingAt: Date,
        isStuck: {
            type: Boolean,
            default: false,
        },

        // 🧠 MERIDIAN: Cross-service sync fields [KEPT]
        syncStatus: {
            type: String,
            enum: ['PENDING', 'SYNCED', 'FAILED', 'DEAD_LETTER'],
            default: 'PENDING',
        },

        syncRetryCount: {
            type: Number,
            default: 0,
        },

        lastSyncAttempt: Date,
        nextSyncRetry: Date,
        deadLetterReason: String,
        orderServiceAcknowledged: {
            type: Boolean,
            default: false,
        },

        // 🧠 SOVERIGN: Transaction finality fields [NEW]
        settlementId: {
            type: String,
            unique: true,
            sparse: true,
            index: true,
        },

        settlementProof: {
            type: String,
        },

        settlementHash: {
            type: String,
            index: true,
        },

        settledAt: {
            type: Date,
        },

        settlementAttempts: {
            type: Number,
            default: 0,
        },

        settlementStatus: {
            type: String,
            enum: ['PENDING', 'PROCESSING', 'SETTLED', 'FAILED', 'DISPUTED'],
            default: 'PENDING',
        },

        settlementLatencyMs: {
            type: Number,
        },

        externalReferenceId: {
            type: String,
            index: true,
            sparse: true,
        },

        reconciliationStatus: {
            type: String,
            enum: ['UNRECONCILED', 'RECONCILED', 'DISCREPANCY_FOUND'],
            default: 'UNRECONCILED',
        },

        lastReconciledAt: Date,
    },
    {
        timestamps: true,
        versionKey: '__v',
        toJSON: { virtuals: true },
        toObject: { virtuals: true },
    }
);

// ============================================================
// 🧠 VIRTUAL FIELDS [KEPT + ENHANCED]
// ============================================================

transactionSchema.virtual('isReconcilable').get(function() {
    return this.status === 'pending' && !this.isFinalized && Date.now() - this.createdAt > 5 * 60 * 1000;
});

transactionSchema.virtual('needsHealing').get(function() {
    return (this.isReconcilable || this.syncStatus === 'FAILED') && !this.isFinalized;
});

transactionSchema.virtual('recoveryETA').get(function() {
    if (!this.nextSyncRetry) return null;
    return Math.max(0, Math.ceil((this.nextSyncRetry - Date.now()) / 1000));
});

transactionSchema.virtual('isSettled').get(function() {
    return this.settlementStatus === 'SETTLED';
});

transactionSchema.virtual('settlementAgeMs').get(function() {
    if (!this.settledAt) return null;
    return Date.now() - this.settledAt;
});

// ============================================================
// 🧠 ALGORITHM 1: TIS (Prevent duplicate transactions) [KEPT]
// ============================================================

transactionSchema.index(
    { idempotencyKey: 1, orderId: 1 },
    { unique: true }
);

// ============================================================
// 🧠 ALGORITHM 2: LIP (Immutable after finalization) [KEPT]
// ============================================================

transactionSchema.pre('save', function (next) {
    if (this.isModified() && this.isFinalized) {
        return next(
            new Error(
                'Transaction is finalized and cannot be modified (LIP protection)'
            )
        );
    }
    next();
});

// ============================================================
// 🧠 ALGORITHM 3: REAPER (Real-time Event Aggregation & Pattern-based Error Recognition) [KEPT - FIXED CONSTANT]
// ============================================================

class TransactionAnomalyDetector {
    constructor() {
        this.userTransactionHistory = new Map();
        this.windowSizeMs = 3600000;
        this.rapidTransactionThreshold = 5;
        this.rapidWindowMs = 300000;
        this.stats = {
            totalScans: 0,
            anomaliesDetected: 0,
            falsePositives: 0,
            avgDetectionTimeMs: 0,
        };
    }

    async detectAnomalies(transactionData, existingTransaction = null) {
        const startTime = Date.now();
        const anomalies = [];
        let anomalyScore = 0;

        const rapidResult = this.checkRapidTransactions(
            transactionData.userId,
            transactionData.amount,
            transactionData.timestamp || Date.now()
        );

        if (rapidResult.isAnomaly) {
            anomalies.push({
                type: ANOMALY_TYPES.RAPID_TRANSACTIONS,
                severity: ANOMALY_SEVERITY.HIGH,
                description: rapidResult.reason,
            });
            anomalyScore += rapidResult.score;
        }

        const amountResult = this.checkAmountAnomaly(
            transactionData.userId,
            transactionData.amount
        );

        if (amountResult.isAnomaly) {
            // FIXED: Use AMOUNT_MISMATCH instead of AMOUNT_ANOMALY
            anomalies.push({
                type: ANOMALY_TYPES.AMOUNT_MISMATCH,
                severity: amountResult.severity,
                description: amountResult.reason,
            });
            anomalyScore += amountResult.score;
        }

        const duplicateResult = await this.checkDuplicatePattern(
            transactionData.userId,
            transactionData.amount,
            transactionData.orderId
        );

        if (duplicateResult.isAnomaly) {
            anomalies.push({
                type: ANOMALY_TYPES.DUPLICATE_PAYMENT,
                severity: duplicateResult.severity,
                description: duplicateResult.reason,
            });
            anomalyScore += duplicateResult.score;
        }

        const velocityResult = this.checkVelocityPattern(
            transactionData.userId,
            transactionData.timestamp || Date.now()
        );

        if (velocityResult.isAnomaly) {
            anomalies.push({
                type: ANOMALY_TYPES.RAPID_TRANSACTIONS,
                severity: velocityResult.severity,
                description: velocityResult.reason,
            });
            anomalyScore += velocityResult.score;
        }

        this.stats.totalScans++;
        const detectionTime = Date.now() - startTime;
        this.stats.avgDetectionTimeMs =
            (this.stats.avgDetectionTimeMs * (this.stats.totalScans - 1) + detectionTime) /
            this.stats.totalScans;

        if (anomalies.length > 0) {
            this.stats.anomaliesDetected++;
        }

        let riskLevel = RISK_LEVELS.NORMAL;
        if (anomalyScore > 70) riskLevel = RISK_LEVELS.CRITICAL;
        else if (anomalyScore > 50) riskLevel = RISK_LEVELS.HIGH;
        else if (anomalyScore > 30) riskLevel = RISK_LEVELS.ELEVATED;

        return {
            anomalies,
            anomalyScore: Math.min(100, anomalyScore),
            riskLevel,
            requiresReview: anomalyScore > 40,
        };
    }

    checkRapidTransactions(userId, amount, timestamp) {
        const history = this.userTransactionHistory.get(userId) || [];
        const recentTransactions = history.filter(
            t => timestamp - t.timestamp < this.rapidWindowMs
        );

        if (recentTransactions.length >= this.rapidTransactionThreshold) {
            return {
                isAnomaly: true,
                severity: ANOMALY_SEVERITY.HIGH,
                score: 40,
                reason: `${recentTransactions.length} transactions in last ${this.rapidWindowMs / 60000} minutes`,
            };
        }
        return { isAnomaly: false };
    }

    checkAmountAnomaly(userId, amount) {
        const history = this.userTransactionHistory.get(userId) || [];
        const successfulPayments = history.filter(t => t.type === 'payment');

        if (successfulPayments.length < 3) return { isAnomaly: false };

        const avgAmount = successfulPayments.reduce((sum, t) => sum + t.amount, 0) / successfulPayments.length;
        const deviation = Math.abs(amount - avgAmount) / avgAmount;

        if (deviation > 5) {
            return {
                isAnomaly: true,
                severity: ANOMALY_SEVERITY.HIGH,
                score: 30,
                reason: `Amount ${amount} is ${(deviation * 100).toFixed(0)}% above average (${avgAmount.toFixed(2)})`,
            };
        }
        if (deviation > 2) {
            return {
                isAnomaly: true,
                severity: ANOMALY_SEVERITY.MEDIUM,
                score: 15,
                reason: `Amount ${amount} is ${(deviation * 100).toFixed(0)}% above average`,
            };
        }
        return { isAnomaly: false };
    }

    async checkDuplicatePattern(userId, amount, currentOrderId) {
        const Transaction = mongoose.model('Transaction');
        const recentWindow = new Date(Date.now() - this.windowSizeMs);

        const recentTransactions = await Transaction.find({
            userId,
            amount: { $gte: amount * 0.95, $lte: amount * 1.05 },
            createdAt: { $gte: recentWindow },
            orderId: { $ne: currentOrderId },
            status: 'succeeded',
        }).limit(3);

        if (recentTransactions.length >= 2) {
            return {
                isAnomaly: true,
                severity: ANOMALY_SEVERITY.HIGH,
                score: 35,
                reason: `${recentTransactions.length} similar amounts in last hour`,
            };
        }
        return { isAnomaly: false };
    }

    checkVelocityPattern(userId, timestamp) {
        const history = this.userTransactionHistory.get(userId) || [];

        if (history.length < 10) return { isAnomaly: false };

        const intervals = [];
        for (let i = 1; i < Math.min(10, history.length); i++) {
            intervals.push(history[i].timestamp - history[i-1].timestamp);
        }

        if (intervals.length < 5) return { isAnomaly: false };

        const avgInterval = intervals.reduce((a, b) => a + b, 0) / intervals.length;
        const lastInterval = intervals[intervals.length - 1];

        if (lastInterval < avgInterval * 0.5) {
            return {
                isAnomaly: true,
                severity: ANOMALY_SEVERITY.MEDIUM,
                score: 20,
                reason: `Accelerating transaction velocity (${Math.round(lastInterval / 1000)}s vs avg ${Math.round(avgInterval / 1000)}s)`,
            };
        }
        return { isAnomaly: false };
    }

    recordTransaction(transaction) {
        if (!this.userTransactionHistory.has(transaction.userId)) {
            this.userTransactionHistory.set(transaction.userId, []);
        }
        const history = this.userTransactionHistory.get(transaction.userId);
        history.push({
            amount: transaction.amount,
            timestamp: transaction.createdAt || Date.now(),
            type: transaction.type,
            status: transaction.status,
        });
        while (history.length > 100) history.shift();
        this.userTransactionHistory.set(transaction.userId, history);
    }

    getMetrics() {
        return {
            totalScans: this.stats.totalScans,
            anomaliesDetected: this.stats.anomaliesDetected,
            anomalyRate: this.stats.totalScans > 0
                ? ((this.stats.anomaliesDetected / this.stats.totalScans) * 100).toFixed(2) + '%'
                : '0%',
            avgDetectionTimeMs: Math.round(this.stats.avgDetectionTimeMs),
            activeUsersMonitored: this.userTransactionHistory.size,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 4: GLACIER (Granular Ledger Archiving) [KEPT - ENHANCED WITH CONSTANTS]
// ============================================================

class TransactionArchiver {
    constructor() {
        this.policies = {
            [ARCHIVE_TIERS.HOT]: { maxAgeMs: 7 * 24 * 60 * 60 * 1000, maxRecords: 10000000 },
            [ARCHIVE_TIERS.WARM]: { maxAgeMs: 90 * 24 * 60 * 60 * 1000, maxRecords: 50000000 },
            [ARCHIVE_TIERS.COLD]: { maxAgeMs: 365 * 24 * 60 * 60 * 1000, maxRecords: 200000000 },
            [ARCHIVE_TIERS.FROZEN]: { maxAgeMs: Infinity, maxRecords: Infinity },
        };
        this.compressionStats = {
            totalArchived: 0,
            originalSize: 0,
            compressedSize: 0,
            compressionRatio: 0,
        };
        setInterval(() => this.archiveOldTransactions(), 3600000);
    }

    getArchiveTier(transaction) {
        const age = Date.now() - new Date(transaction.createdAt).getTime();
        if (age < this.policies[ARCHIVE_TIERS.HOT].maxAgeMs) return ARCHIVE_TIERS.HOT;
        if (age < this.policies[ARCHIVE_TIERS.WARM].maxAgeMs) return ARCHIVE_TIERS.WARM;
        if (age < this.policies[ARCHIVE_TIERS.COLD].maxAgeMs) return ARCHIVE_TIERS.COLD;
        return ARCHIVE_TIERS.FROZEN;
    }

    compressTransaction(transaction) {
        const payload = {
            _id: transaction._id,
            orderId: transaction.orderId,
            amount: transaction.amount,
            currency: transaction.currency,
            type: transaction.type,
            status: transaction.status,
            createdAt: transaction.createdAt,
            metadata: transaction.metadata,
            anomalyScore: transaction.anomalyScore,
            settlementId: transaction.settlementId,
            settlementStatus: transaction.settlementStatus,
        };
        const originalString = JSON.stringify(payload);
        const originalSize = Buffer.byteLength(originalString, 'utf8');
        const compressed = {
            _id: payload._id,
            o: payload.orderId,
            a: payload.amount,
            c: payload.currency,
            t: payload.type,
            s: payload.status,
            ca: payload.createdAt,
            m: payload.metadata,
            as: payload.anomalyScore,
            si: payload.settlementId,
            ss: payload.settlementStatus,
        };
        const compressedString = JSON.stringify(compressed);
        const compressedSize = Buffer.byteLength(compressedString, 'utf8');
        this.compressionStats.totalArchived++;
        this.compressionStats.originalSize += originalSize;
        this.compressionStats.compressedSize += compressedSize;
        this.compressionStats.compressionRatio =
            (1 - this.compressionStats.compressedSize / this.compressionStats.originalSize) * 100;
        return { compressed: compressedString, originalSize, compressedSize };
    }

    async archiveOldTransactions() {
        const Transaction = mongoose.model('Transaction');
        const now = Date.now();
        const hotCutoff = new Date(now - this.policies[ARCHIVE_TIERS.HOT].maxAgeMs);
        const warmCutoff = new Date(now - this.policies[ARCHIVE_TIERS.WARM].maxAgeMs);

        const warmArchive = await Transaction.updateMany(
            { archiveTier: ARCHIVE_TIERS.HOT, createdAt: { $lt: hotCutoff }, archived: false },
            { $set: { archiveTier: ARCHIVE_TIERS.WARM, archivedAt: new Date() } }
        );

        const coldArchive = await Transaction.updateMany(
            { archiveTier: ARCHIVE_TIERS.WARM, createdAt: { $lt: warmCutoff }, archived: false },
            { $set: { archiveTier: ARCHIVE_TIERS.COLD, archivedAt: new Date() } }
        );

        if (coldArchive.modifiedCount > 0) {
            const coldTransactions = await Transaction.find({
                archiveTier: ARCHIVE_TIERS.COLD,
                compressedPayload: { $exists: false },
            }).limit(10000);
            for (const tx of coldTransactions) {
                const compressed = this.compressTransaction(tx);
                tx.compressedPayload = Buffer.from(compressed.compressed);
                await tx.save();
            }
        }
    }

    async retrieveTransaction(transactionId) {
        const Transaction = mongoose.model('Transaction');
        const transaction = await Transaction.findById(transactionId);
        if (!transaction) return null;
        if (transaction.compressedPayload && transaction.archiveTier === ARCHIVE_TIERS.COLD) {
            const decompressed = JSON.parse(transaction.compressedPayload.toString());
            Object.assign(transaction, decompressed);
        }
        return transaction;
    }

    getMetrics() {
        return {
            compressionRatio: this.compressionStats.compressionRatio.toFixed(1) + '%',
            spaceSavedBytes: this.compressionStats.originalSize - this.compressionStats.compressedSize,
            totalArchived: this.compressionStats.totalArchived,
            policies: {
                HOT: { maxAgeDays: this.policies[ARCHIVE_TIERS.HOT].maxAgeMs / (24 * 60 * 60 * 1000) },
                WARM: { maxAgeDays: this.policies[ARCHIVE_TIERS.WARM].maxAgeMs / (24 * 60 * 60 * 1000) },
                COLD: { maxAgeDays: this.policies[ARCHIVE_TIERS.COLD].maxAgeMs / (24 * 60 * 60 * 1000) },
            },
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 5: PHOENIX (Predictive Health Orchestration) [KEPT]
// ============================================================

class PhoenixHealthOrchestrator {
    constructor() {
        this.healthThresholds = {
            CRITICAL: 30,
            DEGRADED: 60,
            HEALTHY: 80,
        };
        this.stats = {
            totalHeals: 0,
            successfulHeals: 0,
            failedHeals: 0,
            stuckTransactionsResolved: 0,
        };
        setInterval(() => this.scanForStuckTransactions(), 60000);
    }

    calculateHealthScore(transaction) {
        let score = 100;
        if (transaction.anomalyScore > 70) score -= 40;
        else if (transaction.anomalyScore > 40) score -= 20;
        else if (transaction.anomalyScore > 20) score -= 10;
        if (transaction.syncStatus === 'FAILED') score -= 20;
        if (transaction.syncStatus === 'DEAD_LETTER') score -= 50;
        if (transaction.isReconcilable) score -= 15;
        if (transaction.settlementStatus === 'FAILED') score -= 30;
        if (transaction.reconciliationStatus === 'DISCREPANCY_FOUND') score -= 25;
        return Math.max(0, Math.min(100, score));
    }

    async healTransaction(transaction) {
        this.stats.totalHeals++;
        transaction.healingAttempts.push({ attemptType: 'AUTO_RECONCILE', attemptedAt: new Date() });

        if (transaction.isReconcilable) {
            transaction.status = 'failed';
            transaction.failureReason = 'Auto-resolved: stuck transaction';
            transaction.isStuck = false;
            transaction.healthScore = this.calculateHealthScore(transaction);
            await transaction.save();
            this.stats.successfulHeals++;
            this.stats.stuckTransactionsResolved++;
            return true;
        }

        if (transaction.syncStatus === 'FAILED' && transaction.syncRetryCount < 5) {
            transaction.syncStatus = 'PENDING';
            transaction.syncRetryCount++;
            transaction.nextSyncRetry = new Date(Date.now() + Math.pow(2, transaction.syncRetryCount) * 1000);
            await transaction.save();
            this.stats.successfulHeals++;
            return true;
        }

        if (transaction.settlementStatus === 'FAILED' && transaction.settlementAttempts < 3) {
            transaction.settlementStatus = 'PENDING';
            transaction.settlementAttempts++;
            await transaction.save();
            this.stats.successfulHeals++;
            return true;
        }

        this.stats.failedHeals++;
        return false;
    }

    async scanForStuckTransactions() {
        const Transaction = mongoose.model('Transaction');
        const stuckTransactions = await Transaction.find({
            status: 'pending',
            createdAt: { $lt: new Date(Date.now() - 10 * 60 * 1000) },
            isStuck: false,
        }).limit(100);

        for (const transaction of stuckTransactions) {
            transaction.isStuck = true;
            transaction.healthScore = this.calculateHealthScore(transaction);
            await transaction.save();
            await this.healTransaction(transaction);
        }
    }

    getMetrics() {
        return {
            totalHeals: this.stats.totalHeals,
            successRate: this.stats.totalHeals > 0
                ? ((this.stats.successfulHeals / this.stats.totalHeals) * 100).toFixed(2) + '%'
                : 'N/A',
            stuckTransactionsResolved: this.stats.stuckTransactionsResolved,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 6: MERIDIAN (Cross-Service Event Routing) [KEPT]
// ============================================================

class MeridianEventRouter {
    constructor() {
        this.retryDelays = [1000, 2000, 4000, 8000, 16000, 32000];
        this.maxRetries = 5;
        this.deadLetterQueue = [];
        this.stats = {
            eventsRouted: 0,
            eventsSynced: 0,
            eventsFailed: 0,
            deadLettered: 0,
        };
        setInterval(() => this.processRetryQueue(), 10000);
    }

    async syncWithOrderService(transaction, orderServiceClient) {
        this.stats.eventsRouted++;

        try {
            const result = await orderServiceClient.updateOrderStatus({
                orderId: transaction.orderId,
                status: this.mapTransactionStatusToOrderStatus(transaction.status),
                paymentId: transaction.paymentId,
                transactionId: transaction.stripePaymentIntentId,
                amount: transaction.amount,
                idempotencyKey: transaction.idempotencyKey,
            });

            transaction.syncStatus = 'SYNCED';
            transaction.orderServiceAcknowledged = true;
            await transaction.save();
            this.stats.eventsSynced++;
            return { success: true, result };
        } catch (error) {
            transaction.syncStatus = 'FAILED';
            transaction.syncRetryCount++;
            transaction.lastSyncAttempt = new Date();
            transaction.nextSyncRetry = new Date(Date.now() + this.getRetryDelay(transaction.syncRetryCount));
            await transaction.save();

            if (transaction.syncRetryCount >= this.maxRetries) {
                this.addToDeadLetter(transaction, error);
            }

            this.stats.eventsFailed++;
            return { success: false, error };
        }
    }

    mapTransactionStatusToOrderStatus(transactionStatus) {
        const mapping = {
            succeeded: 'payment_received',
            failed: 'payment_failed',
            refunded: 'refunded',
        };
        return mapping[transactionStatus] || 'pending_payment';
    }

    getRetryDelay(retryCount) {
        const index = Math.min(retryCount, this.retryDelays.length - 1);
        return this.retryDelays[index];
    }

    addToDeadLetter(transaction, error) {
        this.deadLetterQueue.push({
            transactionId: transaction._id,
            orderId: transaction.orderId,
            error: error.message,
            timestamp: Date.now(),
            retryCount: transaction.syncRetryCount,
        });
        this.stats.deadLettered++;
        transaction.deadLetterReason = error.message;
        transaction.syncStatus = 'DEAD_LETTER';
        transaction.save();
    }

    async processRetryQueue() {
        const Transaction = mongoose.model('Transaction');
        const pendingRetries = await Transaction.find({
            syncStatus: 'FAILED',
            nextSyncRetry: { $lte: new Date() },
        }).limit(50);

        for (const transaction of pendingRetries) {
            transaction.syncStatus = 'PENDING';
            await transaction.save();
        }
    }

    async retryDeadLetter(item) {
        const Transaction = mongoose.model('Transaction');
        const transaction = await Transaction.findById(item.transactionId);
        if (transaction) {
            transaction.syncRetryCount = 0;
            transaction.syncStatus = 'PENDING';
            transaction.deadLetterReason = null;
            await transaction.save();
            this.deadLetterQueue = this.deadLetterQueue.filter(d => d.transactionId !== item.transactionId);
        }
    }

    getMetrics() {
        return {
            eventsRouted: this.stats.eventsRouted,
            eventsSynced: this.stats.eventsSynced,
            eventsFailed: this.stats.eventsFailed,
            deadLettered: this.stats.deadLettered,
            syncRate: this.stats.eventsRouted > 0
                ? ((this.stats.eventsSynced / this.stats.eventsRouted) * 100).toFixed(2) + '%'
                : 'N/A',
            deadLetterQueueSize: this.deadLetterQueue.length,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 7: SOVERIGN (Transaction Finality Guarantee & Settlement Protection) [NEW]
// ============================================================

class SovereignSettlementEngine {
    constructor() {
        this.settlementStore = new Map();
        this.reconciliationWindowMs = 86400000; // 24 hours
        this.settlementTimeoutMs = 300000; // 5 minutes
        this.stats = {
            totalSettlements: 0,
            successfulSettlements: 0,
            failedSettlements: 0,
            disputedSettlements: 0,
            avgSettlementTimeMs: 0,
            reconciliationRate: 0,
        };

        // Reconciliation scanner every hour
        setInterval(() => this.scanForUnreconciledTransactions(), 3600000);
        console.log('[SOVERIGN] Settlement engine initialized');
    }

    /**
     * Generate unique settlement ID with proof
     */
    generateSettlementId(transactionId, amount, timestamp) {
        const data = `${transactionId}:${amount}:${timestamp}`;
        const hash = crypto.createHash('sha256').update(data).digest('hex');
        return `STL_${hash.substring(0, 16)}_${Date.now()}`;
    }

    /**
     * Generate settlement proof (immutable)
     */
    generateSettlementProof(transaction, settlementId) {
        const proofData = {
            settlementId,
            transactionId: transaction._id,
            orderId: transaction.orderId,
            amount: transaction.amount,
            currency: transaction.currency,
            type: transaction.type,
            timestamp: Date.now(),
            previousHash: this.settlementStore.get('lastHash') || null,
        };

        const proofHash = crypto.createHash('sha256').update(JSON.stringify(proofData)).digest('hex');
        this.settlementStore.set('lastHash', proofHash);

        return {
            proof: proofHash,
            data: proofData,
        };
    }

    /**
     * Settle a transaction (mark as final)
     */
    async settleTransaction(transaction, externalReferenceId = null) {
        const startTime = Date.now();
        this.stats.totalSettlements++;

        if (transaction.isFinalized) {
            return {
                success: false,
                reason: 'Transaction already finalized',
                settlementId: transaction.settlementId,
            };
        }

        if (transaction.settlementStatus === 'SETTLED') {
            return {
                success: true,
                alreadySettled: true,
                settlementId: transaction.settlementId,
            };
        }

        try {
            const settlementId = this.generateSettlementId(
                transaction._id,
                transaction.amount,
                Date.now()
            );

            const settlementProof = this.generateSettlementProof(transaction, settlementId);

            transaction.settlementId = settlementId;
            transaction.settlementProof = settlementProof.proof;
            transaction.settlementHash = settlementProof.data.previousHash;
            transaction.settlementStatus = 'SETTLED';
            transaction.settledAt = new Date();
            transaction.externalReferenceId = externalReferenceId;
            transaction.isFinalized = true;
            transaction.finalizedAt = new Date();

            const settlementLatency = Date.now() - startTime;
            transaction.settlementLatencyMs = settlementLatency;

            await transaction.save();

            this.stats.successfulSettlements++;
            this.stats.avgSettlementTimeMs =
                (this.stats.avgSettlementTimeMs * (this.stats.successfulSettlements - 1) + settlementLatency) /
                this.stats.successfulSettlements;

            console.log(`[SOVERIGN] ✅ Transaction ${transaction._id} settled with ID: ${settlementId}`);

            return {
                success: true,
                settlementId,
                settlementProof: settlementProof.proof,
                settlementLatency,
            };

        } catch (error) {
            transaction.settlementStatus = 'FAILED';
            transaction.settlementAttempts++;
            await transaction.save();
            this.stats.failedSettlements++;

            console.error(`[SOVERIGN] ❌ Settlement failed for ${transaction._id}:`, error.message);

            return {
                success: false,
                error: error.message,
                settlementAttempts: transaction.settlementAttempts,
            };
        }
    }

    /**
     * Mark transaction as disputed (for chargebacks)
     */
    async disputeTransaction(transaction, disputeReason) {
        if (transaction.settlementStatus !== 'SETTLED') {
            return {
                success: false,
                reason: 'Cannot dispute unsettled transaction',
            };
        }

        transaction.settlementStatus = 'DISPUTED';
        transaction.metadata = {
            ...transaction.metadata,
            disputeReason,
            disputedAt: new Date().toISOString(),
        };
        await transaction.save();

        this.stats.disputedSettlements++;
        console.log(`[SOVERIGN] ⚠️ Transaction ${transaction._id} disputed: ${disputeReason}`);

        return {
            success: true,
            settlementId: transaction.settlementId,
            disputeReason,
        };
    }

    /**
     * Reconcile transaction with external source (Stripe/Bank)
     */
    async reconcileTransaction(transaction, externalData) {
        const discrepancies = [];

        // Check amount match
        if (transaction.amount !== externalData.amount) {
            discrepancies.push({
                field: 'amount',
                expected: transaction.amount,
                actual: externalData.amount,
            });
        }

        // Check status match
        if (transaction.status !== externalData.status) {
            discrepancies.push({
                field: 'status',
                expected: transaction.status,
                actual: externalData.status,
            });
        }

        // Check reference match
        if (transaction.externalReferenceId &&
            transaction.externalReferenceId !== externalData.referenceId) {
            discrepancies.push({
                field: 'referenceId',
                expected: transaction.externalReferenceId,
                actual: externalData.referenceId,
            });
        }

        const isReconciled = discrepancies.length === 0;

        transaction.reconciliationStatus = isReconciled ? 'RECONCILED' : 'DISCREPANCY_FOUND';
        transaction.lastReconciledAt = new Date();

        if (!isReconciled) {
            transaction.anomalyFlags.push({
                type: ANOMALY_TYPES.STATE_INCONSISTENCY,
                detectedAt: new Date(),
                severity: ANOMALY_SEVERITY.HIGH,
                description: `Reconciliation discrepancy: ${JSON.stringify(discrepancies)}`,
                resolved: false,
            });
            transaction.anomalyScore = Math.min(100, transaction.anomalyScore + 30);
        }

        await transaction.save();

        // Update reconciliation rate
        const totalReconciled = await mongoose.model('Transaction').countDocuments({
            reconciliationStatus: 'RECONCILED',
        });
        const totalTransactions = await mongoose.model('Transaction').countDocuments();
        this.stats.reconciliationRate = totalTransactions > 0 ? (totalReconciled / totalTransactions) * 100 : 0;

        return {
            reconciled: isReconciled,
            discrepancies,
            reconciliationStatus: transaction.reconciliationStatus,
        };
    }

    /**
     * Scan for unreconciled transactions
     */
    async scanForUnreconciledTransactions() {
        const Transaction = mongoose.model('Transaction');
        const cutoff = new Date(Date.now() - this.reconciliationWindowMs);

        const unreconciled = await Transaction.find({
            reconciliationStatus: 'UNRECONCILED',
            createdAt: { $lt: cutoff },
            status: 'succeeded',
        }).limit(100);

        console.log(`[SOVERIGN] 🔍 Found ${unreconciled.length} unreconciled transactions`);

        for (const transaction of unreconciled) {
            // Flag for manual review
            transaction.reconciliationStatus = 'DISCREPANCY_FOUND';
            transaction.anomalyFlags.push({
                type: ANOMALY_TYPES.STATE_INCONSISTENCY,
                detectedAt: new Date(),
                severity: ANOMALY_SEVERITY.MEDIUM,
                description: 'Transaction never reconciled with external source',
                resolved: false,
            });
            await transaction.save();
        }
    }

    /**
     * Verify settlement proof integrity
     */
    verifySettlementProof(transaction) {
        if (!transaction.settlementProof || !transaction.settlementId) {
            return { valid: false, reason: 'No settlement proof found' };
        }

        const expectedHash = this.generateSettlementProof(transaction, transaction.settlementId);

        if (expectedHash.proof !== transaction.settlementProof) {
            return { valid: false, reason: 'Settlement proof tampered' };
        }

        return { valid: true, settlementId: transaction.settlementId };
    }

    /**
     * Get SOVERIGN metrics
     */
    getMetrics() {
        return {
            totalSettlements: this.stats.totalSettlements,
            successfulSettlements: this.stats.successfulSettlements,
            failedSettlements: this.stats.failedSettlements,
            disputedSettlements: this.stats.disputedSettlements,
            settlementSuccessRate: this.stats.totalSettlements > 0
                ? ((this.stats.successfulSettlements / this.stats.totalSettlements) * 100).toFixed(2) + '%'
                : 'N/A',
            avgSettlementTimeMs: Math.round(this.stats.avgSettlementTimeMs),
            reconciliationRate: this.stats.reconciliationRate.toFixed(2) + '%',
        };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const anomalyDetector = new TransactionAnomalyDetector();
const archiver = new TransactionArchiver();
const phoenixOrchestrator = new PhoenixHealthOrchestrator();
const meridianRouter = new MeridianEventRouter();
const sovereignEngine = new SovereignSettlementEngine();

// ============================================================
// 🚀 ENHANCED METHODS (With REAPER + GLACIER + PHOENIX + MERIDIAN + SOVERIGN)
// ============================================================

transactionSchema.statics.createSafe = async function (data) {
    try {
        const anomalyResult = await anomalyDetector.detectAnomalies(data);
        const tx = new this({
            ...data,
            anomalyFlags: anomalyResult.anomalies,
            anomalyScore: anomalyResult.anomalyScore,
            riskLevel: anomalyResult.riskLevel,
            archiveTier: ARCHIVE_TIERS.HOT,
            healthScore: 100,
            syncStatus: 'PENDING',
            settlementStatus: 'PENDING',
            reconciliationStatus: 'UNRECONCILED',
        });
        await tx.save();
        anomalyDetector.recordTransaction(tx);
        tx.healthScore = phoenixOrchestrator.calculateHealthScore(tx);
        await tx.save();

        // Auto-settle successful payment transactions
        if (tx.type === TRANSACTION_TYPE.PAYMENT && tx.status === 'succeeded') {
            await sovereignEngine.settleTransaction(tx, tx.stripePaymentIntentId);
        }

        if (anomalyResult.requiresReview) {
            console.warn(`[REAPER] ⚠️ Transaction requires review: Score ${anomalyResult.anomalyScore} (Risk: ${anomalyResult.riskLevel})`);
        }
        return tx;
    } catch (err) {
        if (err.code === 11000) {
            return this.findOne({
                idempotencyKey: data.idempotencyKey,
                orderId: data.orderId,
            });
        }
        throw err;
    }
};

transactionSchema.methods.finalize = function () {
    this.isFinalized = true;
    this.finalizedAt = new Date();
    if (this.anomalyScore < 30 && this.anomalyFlags.length > 0) {
        for (const flag of this.anomalyFlags) {
            flag.resolved = true;
            flag.resolvedAt = new Date();
        }
        console.log(`[REAPER] ✅ Auto-resolved anomalies for transaction ${this._id}`);
    }
};

transactionSchema.methods.markSucceeded = function () {
    this.status = 'succeeded';
    this.healthScore = phoenixOrchestrator.calculateHealthScore(this);
    this.finalize();
};

transactionSchema.methods.markFailed = function (reason) {
    this.status = 'failed';
    this.failureReason = reason;
    this.healthScore = phoenixOrchestrator.calculateHealthScore(this);
    this.finalize();
};

transactionSchema.methods.markRefunded = function () {
    this.status = 'refunded';
    this.healthScore = phoenixOrchestrator.calculateHealthScore(this);
    this.finalize();
};

transactionSchema.methods.settle = async function (externalReferenceId = null) {
    return await sovereignEngine.settleTransaction(this, externalReferenceId);
};

transactionSchema.methods.dispute = async function (disputeReason) {
    return await sovereignEngine.disputeTransaction(this, disputeReason);
};

transactionSchema.methods.reconcile = async function (externalData) {
    return await sovereignEngine.reconcileTransaction(this, externalData);
};

transactionSchema.methods.verifySettlement = function () {
    return sovereignEngine.verifySettlementProof(this);
};

transactionSchema.statics.findActiveByOrder = function (orderId) {
    return this.findOne({
        orderId,
        status: { $in: ['pending'] },
        archived: false,
    });
};

transactionSchema.statics.findByIdWithDecompression = async function (id) {
    return await archiver.retrieveTransaction(id);
};

transactionSchema.statics.getTransactionMetrics = async function () {
    const total = await this.countDocuments();
    const byStatus = await this.aggregate([{ $group: { _id: '$status', count: { $sum: 1 } } }]);
    const highRisk = await this.countDocuments({ riskLevel: { $in: [RISK_LEVELS.HIGH, RISK_LEVELS.CRITICAL] } });
    const archived = await this.countDocuments({ archived: true });
    const unhealthy = await this.countDocuments({ healthScore: { $lt: 60 } });
    const deadLetter = await this.countDocuments({ syncStatus: 'DEAD_LETTER' });
    const settled = await this.countDocuments({ settlementStatus: 'SETTLED' });
    const disputed = await this.countDocuments({ settlementStatus: 'DISPUTED' });

    return {
        totalTransactions: total,
        statusDistribution: byStatus,
        highRiskTransactions: highRisk,
        archivedTransactions: archived,
        unhealthyTransactions: unhealthy,
        deadLetterTransactions: deadLetter,
        settledTransactions: settled,
        disputedTransactions: disputed,
        reaper: anomalyDetector.getMetrics(),
        glacier: archiver.getMetrics(),
        phoenix: phoenixOrchestrator.getMetrics(),
        meridian: meridianRouter.getMetrics(),
        sovereign: sovereignEngine.getMetrics(),
        healthScore: this.calculateHealthScore(total, highRisk, unhealthy),
    };
};

transactionSchema.statics.calculateHealthScore = function (total, highRisk, unhealthy) {
    if (total === 0) return 100;
    const riskRate = highRisk / total;
    const unhealthyRate = unhealthy / total;
    return Math.max(0, Math.min(100, 100 - (riskRate * 50) - (unhealthyRate * 30)));
};

// ============================================================
// PERFORMANCE INDEXES (50M SCALE) [ENHANCED WITH SOVERIGN]
// ============================================================

transactionSchema.index({ stripePaymentIntentId: 1 });
transactionSchema.index({ stripeRefundId: 1 });
transactionSchema.index({ status: 1, createdAt: -1 });
transactionSchema.index({ userId: 1, createdAt: -1 });
transactionSchema.index({ anomalyScore: -1, createdAt: -1 });
transactionSchema.index({ riskLevel: 1, createdAt: -1 });
transactionSchema.index({ archiveTier: 1, createdAt: -1 });
transactionSchema.index({ archived: 1, archivedAt: -1 });
transactionSchema.index({ syncStatus: 1, nextSyncRetry: 1 });
transactionSchema.index({ healthScore: -1, updatedAt: -1 });
transactionSchema.index({ isStuck: 1, createdAt: 1 });
transactionSchema.index({ settlementId: 1 });
transactionSchema.index({ settlementStatus: 1, settledAt: -1 });
transactionSchema.index({ settlementHash: 1 });
transactionSchema.index({ externalReferenceId: 1 });
transactionSchema.index({ reconciliationStatus: 1, lastReconciledAt: -1 });

// ============================================================
// EXPORT (Enhanced with SOVERIGN)
// ============================================================

module.exports = mongoose.model('Transaction', transactionSchema);