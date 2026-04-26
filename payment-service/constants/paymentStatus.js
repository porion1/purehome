/**
 * ============================================================
 * 💳 PAYMENT STATUS CONSTANTS — FINITE STATE MACHINE
 * ============================================================
 *
 * PURPOSE:
 * - Centralized payment state definitions
 * - State transition validation rules
 * - Human-readable status labels
 *
 * SCALE TARGET:
 * - 50M+ concurrent payments
 * - Zero invalid state transitions
 *
 * ============================================================
 */

/**
 * Payment status enum
 * @readonly
 * @enum {string}
 */
const PAYMENT_STATUS = {
    CREATED: 'created',
    PENDING: 'pending',
    PROCESSING: 'processing',
    SUCCEEDED: 'succeeded',
    FAILED: 'failed',
    CANCELLED: 'cancelled',
    REFUNDED: 'refunded',
    PARTIALLY_REFUNDED: 'partially_refunded',
    EXPIRED: 'expired',
    REQUIRES_ACTION: 'requires_action',
};

/**
 * Payment status priority (higher number = more final)
 * Used for state transition validation
 */
const PAYMENT_STATUS_PRIORITY = {
    [PAYMENT_STATUS.CREATED]: 1,
    [PAYMENT_STATUS.PENDING]: 2,
    [PAYMENT_STATUS.REQUIRES_ACTION]: 2,
    [PAYMENT_STATUS.PROCESSING]: 3,
    [PAYMENT_STATUS.SUCCEEDED]: 4,
    [PAYMENT_STATUS.FAILED]: 4,
    [PAYMENT_STATUS.CANCELLED]: 4,
    [PAYMENT_STATUS.PARTIALLY_REFUNDED]: 5,
    [PAYMENT_STATUS.REFUNDED]: 6,
    [PAYMENT_STATUS.EXPIRED]: 4,
};

/**
 * Allowed state transitions
 * Format: currentState -> [allowedNextStates]
 */
const PAYMENT_ALLOWED_TRANSITIONS = {
    [PAYMENT_STATUS.CREATED]: [
        PAYMENT_STATUS.PENDING,
        PAYMENT_STATUS.PROCESSING,
        PAYMENT_STATUS.CANCELLED,
        PAYMENT_STATUS.EXPIRED,
    ],
    [PAYMENT_STATUS.PENDING]: [
        PAYMENT_STATUS.PROCESSING,
        PAYMENT_STATUS.SUCCEEDED,
        PAYMENT_STATUS.FAILED,
        PAYMENT_STATUS.CANCELLED,
        PAYMENT_STATUS.EXPIRED,
    ],
    [PAYMENT_STATUS.REQUIRES_ACTION]: [
        PAYMENT_STATUS.PROCESSING,
        PAYMENT_STATUS.SUCCEEDED,
        PAYMENT_STATUS.FAILED,
        PAYMENT_STATUS.CANCELLED,
        PAYMENT_STATUS.EXPIRED,
    ],
    [PAYMENT_STATUS.PROCESSING]: [
        PAYMENT_STATUS.SUCCEEDED,
        PAYMENT_STATUS.FAILED,
        PAYMENT_STATUS.CANCELLED,
    ],
    [PAYMENT_STATUS.SUCCEEDED]: [
        PAYMENT_STATUS.PARTIALLY_REFUNDED,
        PAYMENT_STATUS.REFUNDED,
    ],
    [PAYMENT_STATUS.FAILED]: [],
    [PAYMENT_STATUS.CANCELLED]: [],
    [PAYMENT_STATUS.PARTIALLY_REFUNDED]: [
        PAYMENT_STATUS.REFUNDED,
    ],
    [PAYMENT_STATUS.REFUNDED]: [],
    [PAYMENT_STATUS.EXPIRED]: [],
};

/**
 * Human-readable status labels
 */
const PAYMENT_STATUS_LABELS = {
    [PAYMENT_STATUS.CREATED]: 'Payment Created',
    [PAYMENT_STATUS.PENDING]: 'Payment Pending',
    [PAYMENT_STATUS.PROCESSING]: 'Payment Processing',
    [PAYMENT_STATUS.SUCCEEDED]: 'Payment Successful',
    [PAYMENT_STATUS.FAILED]: 'Payment Failed',
    [PAYMENT_STATUS.CANCELLED]: 'Payment Cancelled',
    [PAYMENT_STATUS.REFUNDED]: 'Fully Refunded',
    [PAYMENT_STATUS.PARTIALLY_REFUNDED]: 'Partially Refunded',
    [PAYMENT_STATUS.EXPIRED]: 'Payment Expired',
    [PAYMENT_STATUS.REQUIRES_ACTION]: 'Action Required',
};

/**
 * Statuses that are considered terminal (no further transitions)
 */
const TERMINAL_PAYMENT_STATUSES = [
    PAYMENT_STATUS.SUCCEEDED,
    PAYMENT_STATUS.FAILED,
    PAYMENT_STATUS.CANCELLED,
    PAYMENT_STATUS.REFUNDED,
    PAYMENT_STATUS.EXPIRED,
];

/**
 * Statuses that are considered successful
 */
const SUCCESS_PAYMENT_STATUSES = [
    PAYMENT_STATUS.SUCCEEDED,
];

/**
 * Statuses that require customer action
 */
const ACTION_REQUIRED_STATUSES = [
    PAYMENT_STATUS.REQUIRES_ACTION,
];

/**
 * Check if a status transition is allowed
 */
const isAllowedTransition = (currentStatus, newStatus) => {
    const allowed = PAYMENT_ALLOWED_TRANSITIONS[currentStatus];
    return allowed ? allowed.includes(newStatus) : false;
};

/**
 * Check if a status is terminal
 */
const isTerminalStatus = (status) => {
    return TERMINAL_PAYMENT_STATUSES.includes(status);
};

/**
 * Check if a status is successful
 */
const isSuccessStatus = (status) => {
    return SUCCESS_PAYMENT_STATUSES.includes(status);
};

/**
 * Get human-readable label for status
 */
const getStatusLabel = (status) => {
    return PAYMENT_STATUS_LABELS[status] || status;
};

module.exports = {
    PAYMENT_STATUS,
    PAYMENT_STATUS_PRIORITY,
    PAYMENT_ALLOWED_TRANSITIONS,
    PAYMENT_STATUS_LABELS,
    TERMINAL_PAYMENT_STATUSES,
    SUCCESS_PAYMENT_STATUSES,
    ACTION_REQUIRED_STATUSES,
    isAllowedTransition,
    isTerminalStatus,
    isSuccessStatus,
    getStatusLabel,
};