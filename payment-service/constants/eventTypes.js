/**
 * ============================================================
 * 📡 EVENT TYPES CONSTANTS — DOMAIN EVENT CONTRACT
 * ============================================================
 *
 * PURPOSE:
 * - Centralized event type definitions
 * - Event versioning and schema references
 * - Event priority mapping for queues
 *
 * SCALE TARGET:
 * - 50M+ events per day
 * - Zero event loss
 *
 * ============================================================
 */

/**
 * Payment domain events
 */
const PAYMENT_EVENTS = {
    // Payment lifecycle events
    PAYMENT_CREATED: 'payment.created',
    PAYMENT_PENDING: 'payment.pending',
    PAYMENT_PROCESSING: 'payment.processing',
    PAYMENT_SUCCEEDED: 'payment.succeeded',
    PAYMENT_FAILED: 'payment.failed',
    PAYMENT_CANCELLED: 'payment.cancelled',
    PAYMENT_EXPIRED: 'payment.expired',
    PAYMENT_REQUIRES_ACTION: 'payment.requires_action',

    // Authorization events
    PAYMENT_AUTHORIZED: 'payment.authorized',
    PAYMENT_CAPTURED: 'payment.captured',

    // Refund events
    REFUND_REQUESTED: 'refund.requested',
    REFUND_PROCESSING: 'refund.processing',
    REFUND_COMPLETED: 'refund.completed',
    REFUND_FAILED: 'refund.failed',

    // Webhook events
    WEBHOOK_RECEIVED: 'webhook.received',
    WEBHOOK_PROCESSED: 'webhook.processed',
    WEBHOOK_FAILED: 'webhook.failed',

    // Idempotency events
    IDEMPOTENCY_HIT: 'idempotency.hit',
    IDEMPOTENCY_MISS: 'idempotency.miss',
    IDEMPOTENCY_LOCK_ACQUIRED: 'idempotency.lock_acquired',
    IDEMPOTENCY_LOCK_RELEASED: 'idempotency.lock_released',

    // Circuit breaker events
    CIRCUIT_OPENED: 'circuit.opened',
    CIRCUIT_CLOSED: 'circuit.closed',
    CIRCUIT_HALF_OPEN: 'circuit.half_open',

    // Anomaly events
    ANOMALY_DETECTED: 'anomaly.detected',
    FRAUD_SUSPECTED: 'fraud.suspected',
};

/**
 * Event priority levels for queue processing
 */
const EVENT_PRIORITY = {
    CRITICAL: 5,    // Payment success/failure - must process immediately
    HIGH: 4,        // Refunds, authorizations
    NORMAL: 3,      // Standard events
    LOW: 2,         // Analytics, metrics
    BACKGROUND: 1,  // Cleanup, archiving
};

/**
 * Event priority mapping
 */
const EVENT_PRIORITY_MAP = {
    [PAYMENT_EVENTS.PAYMENT_SUCCEEDED]: EVENT_PRIORITY.CRITICAL,
    [PAYMENT_EVENTS.PAYMENT_FAILED]: EVENT_PRIORITY.CRITICAL,
    [PAYMENT_EVENTS.REFUND_COMPLETED]: EVENT_PRIORITY.HIGH,
    [PAYMENT_EVENTS.REFUND_REQUESTED]: EVENT_PRIORITY.HIGH,
    [PAYMENT_EVENTS.PAYMENT_AUTHORIZED]: EVENT_PRIORITY.HIGH,
    [PAYMENT_EVENTS.PAYMENT_CAPTURED]: EVENT_PRIORITY.NORMAL,
    [PAYMENT_EVENTS.PAYMENT_CREATED]: EVENT_PRIORITY.NORMAL,
    [PAYMENT_EVENTS.PAYMENT_PENDING]: EVENT_PRIORITY.NORMAL,
    [PAYMENT_EVENTS.WEBHOOK_RECEIVED]: EVENT_PRIORITY.NORMAL,
    [PAYMENT_EVENTS.REFUND_PROCESSING]: EVENT_PRIORITY.NORMAL,
    [PAYMENT_EVENTS.PAYMENT_CANCELLED]: EVENT_PRIORITY.LOW,
    [PAYMENT_EVENTS.PAYMENT_EXPIRED]: EVENT_PRIORITY.LOW,
    [PAYMENT_EVENTS.REFUND_FAILED]: EVENT_PRIORITY.LOW,
    [PAYMENT_EVENTS.WEBHOOK_FAILED]: EVENT_PRIORITY.LOW,
    [PAYMENT_EVENTS.IDEMPOTENCY_HIT]: EVENT_PRIORITY.BACKGROUND,
    [PAYMENT_EVENTS.IDEMPOTENCY_MISS]: EVENT_PRIORITY.BACKGROUND,
    [PAYMENT_EVENTS.CIRCUIT_OPENED]: EVENT_PRIORITY.BACKGROUND,
    [PAYMENT_EVENTS.CIRCUIT_CLOSED]: EVENT_PRIORITY.BACKGROUND,
    [PAYMENT_EVENTS.ANOMALY_DETECTED]: EVENT_PRIORITY.BACKGROUND,
};

/**
 * Event version mapping (for schema evolution)
 */
const EVENT_VERSIONS = {
    [PAYMENT_EVENTS.PAYMENT_CREATED]: 1,
    [PAYMENT_EVENTS.PAYMENT_SUCCEEDED]: 1,
    [PAYMENT_EVENTS.PAYMENT_FAILED]: 1,
    [PAYMENT_EVENTS.REFUND_REQUESTED]: 1,
    [PAYMENT_EVENTS.REFUND_COMPLETED]: 1,
    [PAYMENT_EVENTS.WEBHOOK_RECEIVED]: 1,
};

/**
 * Event retention periods (milliseconds)
 */
const EVENT_RETENTION = {
    [EVENT_PRIORITY.CRITICAL]: 90 * 24 * 60 * 60 * 1000, // 90 days
    [EVENT_PRIORITY.HIGH]: 30 * 24 * 60 * 60 * 1000,      // 30 days
    [EVENT_PRIORITY.NORMAL]: 7 * 24 * 60 * 60 * 1000,     // 7 days
    [EVENT_PRIORITY.LOW]: 1 * 24 * 60 * 60 * 1000,        // 1 day
    [EVENT_PRIORITY.BACKGROUND]: 24 * 60 * 60 * 1000,      // 24 hours
};

/**
 * Get event priority
 */
const getEventPriority = (eventType) => {
    return EVENT_PRIORITY_MAP[eventType] || EVENT_PRIORITY.NORMAL;
};

/**
 * Get event retention period
 */
const getEventRetention = (eventType) => {
    const priority = getEventPriority(eventType);
    return EVENT_RETENTION[priority] || EVENT_RETENTION[EVENT_PRIORITY.NORMAL];
};

/**
 * Get event version
 */
const getEventVersion = (eventType) => {
    return EVENT_VERSIONS[eventType] || 1;
};

/**
 * Check if event requires immediate processing
 */
const isCriticalEvent = (eventType) => {
    return getEventPriority(eventType) === EVENT_PRIORITY.CRITICAL;
};

module.exports = {
    PAYMENT_EVENTS,
    EVENT_PRIORITY,
    EVENT_PRIORITY_MAP,
    EVENT_VERSIONS,
    EVENT_RETENTION,
    getEventPriority,
    getEventRetention,
    getEventVersion,
    isCriticalEvent,
};