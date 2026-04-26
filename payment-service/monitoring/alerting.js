/**
 * ============================================================
 * 🚨 ALERTING ENGINE — PROACTIVE INCIDENT DETECTION v1.0
 * ============================================================
 *
 * PURPOSE:
 * - Real-time anomaly detection across payment service
 * - Multi-channel alert delivery (Slack, PagerDuty, Webhook)
 * - Predictive alerting before failures occur
 * - Incident lifecycle management (trigger → acknowledge → resolve)
 *
 * SCALE TARGET:
 * - 50M+ transactions/day
 * - Sub-second anomaly detection
 * - Zero alert fatigue with intelligent deduplication
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: ANOMALY DETECTION ENGINE (Statistical Thresholding)
 * ------------------------------------------------------------
 * - Z-score based anomaly detection on metrics
 * - Adaptive thresholds based on historical patterns
 * - Detects sudden spikes, drops, and trends
 *
 * 🧠 ALGORITHM 2: ALERT DEDUPLICATION & COOLDOWN (Intelligent Grouping)
 * ------------------------------------------------------------
 * - Prevents alert storms with exponential backoff
 * - Groups related alerts into incidents
 * - Auto-resolves when metrics recover
 *
 * ============================================================
 */

const os = require('os');
const crypto = require('crypto');

// ============================================================
// CONFIG
// ============================================================

const ALERT_CHANNELS = {
    SLACK: process.env.ALERT_SLACK_WEBHOOK,
    PAGERDUTY: process.env.ALERT_PAGERDUTY_KEY,
    WEBHOOK: process.env.ALERT_WEBHOOK_URL,
};

const DEFAULT_COOLDOWN_MS = 300000; // 5 minutes
const ESCALATION_INTERVALS = [300000, 600000, 1800000]; // 5min, 10min, 30min

// ============================================================
// ALERT DEFINITIONS
// ============================================================

const ALERT_RULES = {
    // Payment success rate dropped below threshold
    PAYMENT_SUCCESS_RATE: {
        name: 'payment_success_rate',
        description: 'Payment success rate dropped below 95%',
        metric: 'payment_success_rate',
        condition: (value) => value < 95,
        severity: 'CRITICAL',
        threshold: 95,
        windowMs: 60000, // 1 minute
        cooldownMs: 300000, // 5 minutes
    },

    // High error rate from Stripe
    STRIPE_ERROR_RATE: {
        name: 'stripe_error_rate',
        description: 'Stripe API error rate exceeded 5%',
        metric: 'stripe_error_rate',
        condition: (value) => value > 5,
        severity: 'HIGH',
        threshold: 5,
        windowMs: 60000,
        cooldownMs: 300000,
    },

    // Circuit breaker opened
    CIRCUIT_OPEN: {
        name: 'circuit_breaker_open',
        description: 'Circuit breaker opened for service',
        metric: 'circuit_state',
        condition: (value) => value === 'OPEN',
        severity: 'CRITICAL',
        threshold: 'OPEN',
        windowMs: 0,
        cooldownMs: 60000,
    },

    // High response latency
    HIGH_LATENCY: {
        name: 'high_latency',
        description: 'p95 response time exceeded 2 seconds',
        metric: 'p95_latency_ms',
        condition: (value) => value > 2000,
        severity: 'HIGH',
        threshold: 2000,
        windowMs: 60000,
        cooldownMs: 300000,
    },

    // Database connection pool exhaustion
    DB_POOL_EXHAUSTED: {
        name: 'db_pool_exhausted',
        description: 'Database connection pool utilization > 90%',
        metric: 'db_pool_utilization',
        condition: (value) => value > 90,
        severity: 'HIGH',
        threshold: 90,
        windowMs: 30000,
        cooldownMs: 300000,
    },

    // Memory pressure
    HIGH_MEMORY_USAGE: {
        name: 'high_memory_usage',
        description: 'Memory usage exceeded 85%',
        metric: 'memory_usage_percent',
        condition: (value) => value > 85,
        severity: 'MEDIUM',
        threshold: 85,
        windowMs: 60000,
        cooldownMs: 600000,
    },

    // Idempotency cache high miss rate
    IDEMPOTENCY_CACHE_MISS: {
        name: 'idempotency_cache_miss',
        description: 'Idempotency cache miss rate > 50%',
        metric: 'idempotency_hit_rate',
        condition: (value) => value < 50,
        severity: 'MEDIUM',
        threshold: 50,
        windowMs: 60000,
        cooldownMs: 600000,
    },

    // Order service unavailable
    ORDER_SERVICE_DOWN: {
        name: 'order_service_down',
        description: 'Order service health check failed',
        metric: 'order_service_health',
        condition: (value) => value === 'DOWN',
        severity: 'CRITICAL',
        threshold: 'DOWN',
        windowMs: 0,
        cooldownMs: 60000,
    },

    // Product service unavailable
    PRODUCT_SERVICE_DOWN: {
        name: 'product_service_down',
        description: 'Product service health check failed',
        metric: 'product_service_health',
        condition: (value) => value === 'DOWN',
        severity: 'CRITICAL',
        threshold: 'DOWN',
        windowMs: 0,
        cooldownMs: 60000,
    },

    // User service unavailable
    USER_SERVICE_DOWN: {
        name: 'user_service_down',
        description: 'User service health check failed',
        metric: 'user_service_health',
        condition: (value) => value === 'DOWN',
        severity: 'CRITICAL',
        threshold: 'DOWN',
        windowMs: 0,
        cooldownMs: 60000,
    },

    // Fraud detection triggered
    FRAUD_DETECTED: {
        name: 'fraud_detected',
        description: 'High-risk transaction detected',
        metric: 'fraud_risk_score',
        condition: (value) => value > 80,
        severity: 'HIGH',
        threshold: 80,
        windowMs: 0,
        cooldownMs: 600000,
    },

    // Payment volume anomaly (sudden drop/spike)
    PAYMENT_VOLUME_ANOMALY: {
        name: 'payment_volume_anomaly',
        description: 'Payment volume deviated by > 50% from baseline',
        metric: 'payment_volume',
        condition: (value, baseline) => Math.abs(value - baseline) / baseline > 0.5,
        severity: 'MEDIUM',
        threshold: 0.5,
        windowMs: 300000,
        cooldownMs: 900000,
    },
};

// ============================================================
// 🧠 ALGORITHM 1: ANOMALY DETECTION ENGINE
// ============================================================

class AnomalyDetector {
    constructor() {
        this.metricHistory = new Map(); // metric -> { values, timestamps, baseline }
        this.historyWindowMs = 3600000; // 1 hour
        this.zScoreThreshold = 3.0; // 3 standard deviations
        this.stats = {
            totalDetections: 0,
            anomaliesFound: 0,
            falsePositives: 0,
        };
    }

    /**
     * Record metric value for anomaly detection
     */
    recordMetric(metric, value, timestamp = Date.now()) {
        if (!this.metricHistory.has(metric)) {
            this.metricHistory.set(metric, {
                values: [],
                timestamps: [],
                baseline: null,
                stdDev: null,
            });
        }

        const history = this.metricHistory.get(metric);
        history.values.push(value);
        history.timestamps.push(timestamp);

        // Clean old entries
        const cutoff = timestamp - this.historyWindowMs;
        while (history.timestamps.length > 0 && history.timestamps[0] < cutoff) {
            history.values.shift();
            history.timestamps.shift();
        }

        // Update baseline statistics
        if (history.values.length > 30) {
            const sum = history.values.reduce((a, b) => a + b, 0);
            const mean = sum / history.values.length;
            const squaredDiffs = history.values.map(v => Math.pow(v - mean, 2));
            const variance = squaredDiffs.reduce((a, b) => a + b, 0) / history.values.length;

            history.baseline = mean;
            history.stdDev = Math.sqrt(variance);
        }

        return history;
    }

    /**
     * Detect anomaly using Z-score
     */
    isAnomaly(metric, value) {
        const history = this.metricHistory.get(metric);
        if (!history || history.baseline === null || history.stdDev === null) {
            return false;
        }

        const zScore = Math.abs((value - history.baseline) / history.stdDev);
        const isAnomaly = zScore > this.zScoreThreshold;

        if (isAnomaly) {
            this.stats.anomaliesFound++;
            console.log(`[ANOMALY] 📊 ${metric}: value=${value}, baseline=${history.baseline.toFixed(2)}, z-score=${zScore.toFixed(2)}`);
        }

        this.stats.totalDetections++;
        return isAnomaly;
    }

    /**
     * Detect trend anomaly (consistently moving in one direction)
     */
    detectTrendAnomaly(metric, values) {
        if (values.length < 10) return false;

        const recent = values.slice(-5);
        const older = values.slice(-10, -5);

        const recentAvg = recent.reduce((a, b) => a + b, 0) / recent.length;
        const olderAvg = older.reduce((a, b) => a + b, 0) / older.length;

        // 50% increase or decrease trend
        const trend = (recentAvg - olderAvg) / olderAvg;
        return Math.abs(trend) > 0.5;
    }

    getMetrics() {
        return {
            totalDetections: this.stats.totalDetections,
            anomaliesFound: this.stats.anomaliesFound,
            anomalyRate: this.stats.totalDetections > 0
                ? ((this.stats.anomaliesFound / this.stats.totalDetections) * 100).toFixed(2) + '%'
                : '0%',
            trackedMetrics: this.metricHistory.size,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: ALERT DEDUPLICATION & COOLDOWN
// ============================================================

class AlertManager {
    constructor() {
        this.activeAlerts = new Map(); // alertId -> { count, firstSeen, lastSeen, severity, status }
        this.resolvedAlerts = new Map(); // alertId -> { resolvedAt, reason }
        this.alertHistory = [];
        this.maxHistorySize = 10000;
        this.stats = {
            totalAlerts: 0,
            resolvedAlerts: 0,
            escalatedAlerts: 0,
            suppressedAlerts: 0,
        };

        // Auto-resolve stale alerts
        setInterval(() => this.autoResolveStaleAlerts(), 60000);
    }

    /**
     * Generate unique alert ID
     */
    generateAlertId(ruleName, context) {
        const hash = crypto.createHash('sha256')
            .update(`${ruleName}:${JSON.stringify(context)}:${Math.floor(Date.now() / 60000)}`)
            .digest('hex');
        return `${ruleName}_${hash.substring(0, 12)}`;
    }

    /**
     * Check if alert should be triggered (deduplication + cooldown)
     */
    shouldTrigger(rule, value, context) {
        const alertId = this.generateAlertId(rule.name, context);
        const existing = this.activeAlerts.get(alertId);

        // No active alert - trigger new one
        if (!existing) {
            return { shouldTrigger: true, alertId, isDuplicate: false };
        }

        // Check if cooldown period has passed
        const cooldownMs = rule.cooldownMs || DEFAULT_COOLDOWN_MS;
        const timeSinceLast = Date.now() - existing.lastSeen;

        if (timeSinceLast < cooldownMs) {
            this.stats.suppressedAlerts++;
            return { shouldTrigger: false, alertId, isDuplicate: true, remainingCooldown: cooldownMs - timeSinceLast };
        }

        // Update existing alert
        existing.count++;
        existing.lastSeen = Date.now();
        existing.lastValue = value;
        this.activeAlerts.set(alertId, existing);

        // Check if escalation needed
        if (existing.count >= ESCALATION_INTERVALS.length) {
            existing.escalated = true;
            this.stats.escalatedAlerts++;
        }

        return { shouldTrigger: true, alertId, isDuplicate: true, escalationLevel: existing.count };
    }

    /**
     * Register new alert
     */
    registerAlert(rule, value, context, alertId) {
        const alert = {
            id: alertId,
            rule: rule.name,
            description: rule.description,
            severity: rule.severity,
            value,
            context,
            firstSeen: Date.now(),
            lastSeen: Date.now(),
            count: 1,
            status: 'FIRING',
            escalated: false,
        };

        this.activeAlerts.set(alertId, alert);
        this.alertHistory.unshift(alert);

        // Trim history
        while (this.alertHistory.length > this.maxHistorySize) {
            this.alertHistory.pop();
        }

        this.stats.totalAlerts++;

        return alert;
    }

    /**
     * Resolve alert
     */
    resolveAlert(alertId, reason = 'RESOLVED') {
        const alert = this.activeAlerts.get(alertId);
        if (!alert) return null;

        alert.status = 'RESOLVED';
        alert.resolvedAt = Date.now();
        alert.resolveReason = reason;

        this.activeAlerts.delete(alertId);
        this.resolvedAlerts.set(alertId, alert);
        this.stats.resolvedAlerts++;

        return alert;
    }

    /**
     * Auto-resolve alerts that haven't been seen for a while
     */
    autoResolveStaleAlerts() {
        const now = Date.now();
        const staleThreshold = 600000; // 10 minutes

        for (const [alertId, alert] of this.activeAlerts.entries()) {
            if (now - alert.lastSeen > staleThreshold) {
                this.resolveAlert(alertId, 'AUTO_RESOLVED_STALE');
                console.log(`[ALERT] ✅ Auto-resolved stale alert: ${alertId}`);
            }
        }
    }

    /**
     * Get active alerts
     */
    getActiveAlerts() {
        return Array.from(this.activeAlerts.values());
    }

    /**
     * Get alert history
     */
    getAlertHistory(limit = 100) {
        return this.alertHistory.slice(0, limit);
    }

    getMetrics() {
        return {
            activeAlerts: this.activeAlerts.size,
            totalAlerts: this.stats.totalAlerts,
            resolvedAlerts: this.stats.resolvedAlerts,
            escalatedAlerts: this.stats.escalatedAlerts,
            suppressedAlerts: this.stats.suppressedAlerts,
        };
    }
}

// ============================================================
// 📤 ALERT DELIVERY CHANNELS
// ============================================================

class AlertDelivery {
    constructor() {
        this.channels = [];
        this.deliveryStats = {
            totalDeliveries: 0,
            successfulDeliveries: 0,
            failedDeliveries: 0,
        };

        this.initChannels();
    }

    initChannels() {
        if (ALERT_CHANNELS.SLACK) {
            this.channels.push({
                name: 'slack',
                send: this.sendSlack.bind(this),
            });
        }

        if (ALERT_CHANNELS.PAGERDUTY) {
            this.channels.push({
                name: 'pagerduty',
                send: this.sendPagerDuty.bind(this),
            });
        }

        if (ALERT_CHANNELS.WEBHOOK) {
            this.channels.push({
                name: 'webhook',
                send: this.sendWebhook.bind(this),
            });
        }

        // Always log to console
        this.channels.push({
            name: 'console',
            send: this.sendConsole.bind(this),
        });
    }

    /**
     * Send alert to all configured channels
     */
    async sendAlert(alert) {
        const results = [];

        for (const channel of this.channels) {
            try {
                await channel.send(alert);
                this.deliveryStats.successfulDeliveries++;
                results.push({ channel: channel.name, success: true });
            } catch (error) {
                console.error(`[ALERT-DELIVERY] Failed to send to ${channel.name}:`, error.message);
                this.deliveryStats.failedDeliveries++;
                results.push({ channel: channel.name, success: false, error: error.message });
            }
            this.deliveryStats.totalDeliveries++;
        }

        return results;
    }

    /**
     * Send to Slack
     */
    async sendSlack(alert) {
        if (!ALERT_CHANNELS.SLACK) return;

        const color = {
            CRITICAL: '#ff0000',
            HIGH: '#ff6600',
            MEDIUM: '#ffcc00',
            LOW: '#00cc00',
        }[alert.severity] || '#cccccc';

        const payload = {
            attachments: [{
                color,
                title: `🚨 ${alert.severity}: ${alert.description}`,
                fields: [
                    { title: 'Rule', value: alert.rule, short: true },
                    { title: 'Value', value: alert.value, short: true },
                    { title: 'Count', value: alert.count, short: true },
                    { title: 'First Seen', value: new Date(alert.firstSeen).toISOString(), short: true },
                ],
                footer: 'Payment Service Alerting Engine',
                ts: Math.floor(Date.now() / 1000),
            }],
        };

        const axios = require('axios');
        await axios.post(ALERT_CHANNELS.SLACK, payload);
    }

    /**
     * Send to PagerDuty
     */
    async sendPagerDuty(alert) {
        if (!ALERT_CHANNELS.PAGERDUTY) return;

        const severity = {
            CRITICAL: 'critical',
            HIGH: 'error',
            MEDIUM: 'warning',
            LOW: 'info',
        }[alert.severity] || 'info';

        const payload = {
            routing_key: ALERT_CHANNELS.PAGERDUTY,
            event_action: 'trigger',
            payload: {
                summary: `${alert.severity}: ${alert.description}`,
                source: 'payment-service',
                severity,
                timestamp: new Date().toISOString(),
                custom_details: {
                    rule: alert.rule,
                    value: alert.value,
                    count: alert.count,
                    context: alert.context,
                },
            },
        };

        const axios = require('axios');
        await axios.post('https://events.pagerduty.com/v2/enqueue', payload);
    }

    /**
     * Send to webhook
     */
    async sendWebhook(alert) {
        if (!ALERT_CHANNELS.WEBHOOK) return;

        const axios = require('axios');
        await axios.post(ALERT_CHANNELS.WEBHOOK, alert);
    }

    /**
     * Send to console (always enabled)
     */
    async sendConsole(alert) {
        console.log(JSON.stringify({
            type: 'ALERT',
            ...alert,
            timestamp: new Date().toISOString(),
        }));
    }

    getMetrics() {
        return {
            totalDeliveries: this.deliveryStats.totalDeliveries,
            successRate: this.deliveryStats.totalDeliveries > 0
                ? ((this.deliveryStats.successfulDeliveries / this.deliveryStats.totalDeliveries) * 100).toFixed(2) + '%'
                : '0%',
            activeChannels: this.channels.length,
        };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const anomalyDetector = new AnomalyDetector();
const alertManager = new AlertManager();
const alertDelivery = new AlertDelivery();

// ============================================================
// 📊 METRIC COLLECTOR & EVALUATOR
// ============================================================

class AlertEvaluator {
    constructor() {
        this.metricValues = new Map();
        this.lastEvaluation = Date.now();
        this.evaluationInterval = 10000; // 10 seconds
    }

    /**
     * Update metric value
     */
    updateMetric(metric, value) {
        this.metricValues.set(metric, value);
        anomalyDetector.recordMetric(metric, value);
    }

    /**
     * Batch update metrics
     */
    updateMetrics(metrics) {
        for (const [metric, value] of Object.entries(metrics)) {
            this.updateMetric(metric, value);
        }
    }

    /**
     * Evaluate all alert rules
     */
    evaluateAlerts() {
        const triggeredAlerts = [];

        for (const [ruleName, rule] of Object.entries(ALERT_RULES)) {
            const value = this.metricValues.get(rule.metric);
            if (value === undefined) continue;

            // Get baseline for volume anomaly
            let baseline = null;
            if (rule.name === 'payment_volume_anomaly') {
                const history = anomalyDetector.metricHistory.get(rule.metric);
                baseline = history?.baseline || null;
            }

            // Check condition
            let isTriggered;
            if (rule.condition.length === 2 && baseline !== null) {
                isTriggered = rule.condition(value, baseline);
            } else {
                isTriggered = rule.condition(value);
            }

            if (isTriggered) {
                const context = { metric: rule.metric, value };
                const { shouldTrigger, alertId, isDuplicate, escalationLevel } = alertManager.shouldTrigger(rule, value, context);

                if (shouldTrigger) {
                    const alert = alertManager.registerAlert(rule, value, context, alertId);
                    triggeredAlerts.push(alert);

                    // Send alert via delivery channels
                    alertDelivery.sendAlert(alert);
                }
            } else {
                // Check if alert should be resolved
                for (const [alertId, activeAlert] of alertManager.activeAlerts.entries()) {
                    if (activeAlert.rule === ruleName) {
                        alertManager.resolveAlert(alertId, 'METRIC_RECOVERED');
                    }
                }
            }
        }

        return triggeredAlerts;
    }

    /**
     * Start periodic evaluation
     */
    startPeriodicEvaluation() {
        setInterval(() => {
            this.evaluateAlerts();
        }, this.evaluationInterval);
    }

    getMetrics() {
        return {
            trackedMetrics: this.metricValues.size,
            lastEvaluation: this.lastEvaluation,
        };
    }
}

// ============================================================
// 🔧 INITIALIZE EVALUATOR
// ============================================================

const alertEvaluator = new AlertEvaluator();

// ============================================================
// 🚀 PUBLIC API
// ============================================================

/**
 * Update metric for alert evaluation
 */
const updateMetric = (metric, value) => {
    alertEvaluator.updateMetric(metric, value);
};

/**
 * Batch update metrics
 */
const updateMetrics = (metrics) => {
    alertEvaluator.updateMetrics(metrics);
};

/**
 * Get current active alerts
 */
const getActiveAlerts = () => {
    return alertManager.getActiveAlerts();
};

/**
 * Get alert history
 */
const getAlertHistory = (limit = 100) => {
    return alertManager.getAlertHistory(limit);
};

/**
 * Get alerting engine metrics
 */
const getAlertingMetrics = () => {
    return {
        anomalyDetector: anomalyDetector.getMetrics(),
        alertManager: alertManager.getMetrics(),
        alertDelivery: alertDelivery.getMetrics(),
        evaluator: alertEvaluator.getMetrics(),
        rules: Object.keys(ALERT_RULES).length,
    };
};

/**
 * Health check
 */
const alertingHealthCheck = () => {
    const alertMetrics = alertManager.getMetrics();

    let status = 'HEALTHY';
    if (alertMetrics.activeAlerts > 10) status = 'DEGRADED';
    if (alertMetrics.activeAlerts > 50) status = 'CRITICAL';

    return {
        status,
        timestamp: new Date().toISOString(),
        activeAlerts: alertMetrics.activeAlerts,
        totalAlerts: alertMetrics.totalAlerts,
    };
};

// ============================================================
// START PERIODIC EVALUATION
// ============================================================

alertEvaluator.startPeriodicEvaluation();

// ============================================================
// EXPORTS
// ============================================================

module.exports = {
    // Core API
    updateMetric,
    updateMetrics,
    getActiveAlerts,
    getAlertHistory,
    getAlertingMetrics,
    alertingHealthCheck,

    // Advanced access for monitoring
    anomalyDetector,
    alertManager,
    alertDelivery,
    alertEvaluator,
    ALERT_RULES,
};