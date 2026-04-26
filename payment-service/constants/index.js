/**
 * ============================================================
 * 📦 CONSTANTS BARREL EXPORT — COMPLETE v2.0
 * ============================================================
 *
 * PURPOSE:
 * - Single entry point for all constants
 * - Simplified imports across the application
 * - 50M+ user scale ready
 *
 * ============================================================
 */

// ============================================================
// 📦 PAYMENT STATUS CONSTANTS
// ============================================================

const {
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
} = require('./paymentStatus');

// ============================================================
// 📦 EVENT TYPES CONSTANTS
// ============================================================

const {
    PAYMENT_EVENTS,
    EVENT_PRIORITY,
    EVENT_PRIORITY_MAP,
    EVENT_VERSIONS,
    EVENT_RETENTION,
    getEventPriority,
    getEventRetention,
    getEventVersion,
    isCriticalEvent,
} = require('./eventTypes');

// ============================================================
// 📦 ERROR CODES CONSTANTS
// ============================================================

const {
    ERROR_CODES,
    ERROR_HTTP_STATUS,
    RETRYABLE_ERROR_CODES,
    CLIENT_ERROR_CODES,
    getHttpStatus,
    isRetryableError,
    isClientError,
    createErrorResponse,
} = require('./errorCodes');

// ============================================================
// 📦 HTTP STATUS CONSTANTS
// ============================================================

const {
    HTTP_STATUS,
    HTTP_STATUS_CATEGORY,
    HTTP_STATUS_MESSAGE,
    getStatusMessage,
    isSuccess: isHttpSuccess,
    isClientError: isHttpClientError,
    isServerError,
    isRetryableStatus,
} = require('./httpStatus');

// ============================================================
// 📦 CONFIG CONSTANTS
// ============================================================

const {
    TIMEOUTS,
    RETRY_CONFIG,
    RATE_LIMITS,
    CIRCUIT_BREAKER_CONFIG,
    HEALTH_CONFIG,
    LOG_CONFIG,
    METRICS_CONFIG,
    TRACING_CONFIG,
    CACHE_CONFIG,
    QUEUE_CONFIG,
    PAYMENT_CONFIG,
    REFUND_CONFIG,
    FEATURE_FLAGS,
    getEnvConfig,
} = require('./config');

// ============================================================
// 🆕 ORDER STATUS CONSTANTS
// ============================================================

const ORDER_STATUS = {
    PENDING_PAYMENT: 'pending_payment',
    PAYMENT_RECEIVED: 'payment_received',
    PROCESSING: 'processing',
    SHIPPED: 'shipped',
    DELIVERED: 'delivered',
    CANCELLED: 'cancelled',
    FAILED: 'failed',
};

const ORDER_TRANSITIONS = {
    [ORDER_STATUS.PENDING_PAYMENT]: [ORDER_STATUS.PAYMENT_RECEIVED, ORDER_STATUS.CANCELLED],
    [ORDER_STATUS.PAYMENT_RECEIVED]: [ORDER_STATUS.PROCESSING, ORDER_STATUS.FAILED],
    [ORDER_STATUS.PROCESSING]: [ORDER_STATUS.SHIPPED, ORDER_STATUS.CANCELLED],
    [ORDER_STATUS.SHIPPED]: [ORDER_STATUS.DELIVERED],
    [ORDER_STATUS.DELIVERED]: [],
    [ORDER_STATUS.CANCELLED]: [],
    [ORDER_STATUS.FAILED]: [],
};

// ============================================================
// 🆕 ANOMALY TYPES & SEVERITY
// ============================================================

const ANOMALY_TYPES = {
    DUPLICATE_PAYMENT: 'DUPLICATE_PAYMENT',
    AMOUNT_MISMATCH: 'AMOUNT_MISMATCH',
    TIMING_ANOMALY: 'TIMING_ANOMALY',
    STATE_INCONSISTENCY: 'STATE_INCONSISTENCY',
    RAPID_TRANSACTIONS: 'RAPID_TRANSACTIONS',
    HIGH_RISK_VELOCITY: 'HIGH_RISK_VELOCITY',
};

const ANOMALY_SEVERITY = {
    LOW: 'LOW',
    MEDIUM: 'MEDIUM',
    HIGH: 'HIGH',
    CRITICAL: 'CRITICAL',
};

// ============================================================
// 🆕 RISK LEVELS
// ============================================================

const RISK_LEVELS = {
    NORMAL: 'NORMAL',
    ELEVATED: 'ELEVATED',
    HIGH: 'HIGH',
    CRITICAL: 'CRITICAL',
};

// ============================================================
// 🆕 TIER LEVELS (FIXES THE ERROR)
// ============================================================

const TIER_LEVELS = {
    HOT: 'HOT',
    WARM: 'WARM',
    COLD: 'COLD',
    FROZEN: 'FROZEN',
};

// ============================================================
// 🆕 ARCHIVE TIERS
// ============================================================

const ARCHIVE_TIERS = {
    HOT: 'HOT',
    WARM: 'WARM',
    COLD: 'COLD',
    FROZEN: 'FROZEN',
};

// ============================================================
// 🆕 TRANSACTION STATUS & TYPE
// ============================================================

const TRANSACTION_STATUS = {
    PENDING: 'pending',
    SUCCEEDED: 'succeeded',
    FAILED: 'failed',
    REFUNDED: 'refunded',
};

const TRANSACTION_TYPE = {
    PAYMENT: 'payment',
    REFUND: 'refund',
};

// ============================================================
// 🆕 REFUND STATUS & TYPE
// ============================================================

const REFUND_STATUS = {
    PENDING: 'pending',
    PROCESSING: 'processing',
    COMPLETED: 'completed',
    FAILED: 'failed',
    CANCELLED: 'cancelled',
};

const REFUND_TYPE = {
    FULL: 'full',
    PARTIAL: 'partial',
};

// ============================================================
// 🆕 WEBHOOK CONSTANTS
// ============================================================

const WEBHOOK_EVENTS = {
    PAYMENT_INTENT_SUCCEEDED: 'payment_intent.succeeded',
    PAYMENT_INTENT_FAILED: 'payment_intent.payment_failed',
    CHARGE_REFUNDED: 'charge.refunded',
    CHECKOUT_COMPLETED: 'checkout.session.completed',
};

const WEBHOOK_PRIORITY = {
    CRITICAL: 5,
    HIGH: 4,
    MEDIUM: 3,
    LOW: 2,
    BACKGROUND: 1,
    NORMAL: 3,
};

const WEBHOOK_STATUS = {
    PENDING: 'pending',
    PROCESSED: 'processed',
    FAILED: 'failed',
    RETRYING: 'retrying',
};

// ============================================================
// 🆕 RATE LIMIT TYPES
// ============================================================

const RATE_LIMIT_TYPES = {
    USER: 'user',
    IP: 'ip',
    ENDPOINT: 'endpoint',
    GLOBAL: 'global',
};

// ============================================================
// 🆕 IP BLOCK REASONS
// ============================================================

const IP_BLOCK_REASONS = {
    RAPID_FIRE: 'RAPID_FIRE',
    ENDPOINT_FLIPPING: 'ENDPOINT_FLIPPING',
    USER_AGENT_ROTATION: 'USER_AGENT_ROTATION',
    SUSPICIOUS_PATTERN: 'SUSPICIOUS_PATTERN',
    MANUAL_BLOCK: 'MANUAL_BLOCK',
};

// ============================================================
// 🆕 AUTH ERRORS
// ============================================================

const AUTH_ERRORS = {
    TOKEN_BLOCKED: 'TOKEN_BLOCKED',
    HIGH_ANOMALY_SCORE: 'HIGH_ANOMALY_SCORE',
};

// ============================================================
// 🆕 IDEMPOTENCY STATUS & LOCK STATES
// ============================================================

const IDEMPOTENCY_STATUS = {
    LOCKED: 'LOCKED',
    PROCESSING: 'PROCESSING',
    COMPLETED: 'COMPLETED',
    FAILED: 'FAILED',
};

const LOCK_STATES = {
    ACQUIRED: 'ACQUIRED',
    RELEASED: 'RELEASED',
    EXPIRED: 'EXPIRED',
};

// ============================================================
// 🆕 CIRCUIT BREAKER STATES
// ============================================================

const CIRCUIT_STATES = {
    CLOSED: 'CLOSED',
    OPEN: 'OPEN',
    HALF_OPEN: 'HALF_OPEN',
    DEGRADED: 'DEGRADED',
};

// ============================================================
// 🆕 HEALTH STATUS
// ============================================================

const HEALTH_STATUS = {
    HEALTHY: 'HEALTHY',
    DEGRADED: 'DEGRADED',
    CRITICAL: 'CRITICAL',
    DOWN: 'DOWN',
};

// ============================================================
// 🆕 SORT OPTIONS
// ============================================================

const SORT_OPTIONS = {
    ASC: 'asc',
    DESC: 'desc',
    NEWEST: 'newest',
    OLDEST: 'oldest',
    HIGHEST_PRICE: 'price_desc',
    LOWEST_PRICE: 'price_asc',
    BEST_SELLING: 'score_desc',
};

// ============================================================
// 🧪 TEST: Verify TIER_LEVELS exists
// ============================================================

if (typeof TIER_LEVELS === 'undefined') {
    console.error('[CONSTANTS] ❌ CRITICAL: TIER_LEVELS is undefined!');
} else {
    console.log('[CONSTANTS] ✅ TIER_LEVELS loaded:', Object.keys(TIER_LEVELS));
}

// ============================================================
// 📤 RE-EXPORT ALL CONSTANTS
// ============================================================

module.exports = {
    // ========================================================
    // Payment status
    // ========================================================
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

    // ========================================================
    // Event types
    // ========================================================
    PAYMENT_EVENTS,
    EVENT_PRIORITY,
    EVENT_PRIORITY_MAP,
    EVENT_VERSIONS,
    EVENT_RETENTION,
    getEventPriority,
    getEventRetention,
    getEventVersion,
    isCriticalEvent,

    // ========================================================
    // Error codes
    // ========================================================
    ERROR_CODES,
    ERROR_HTTP_STATUS,
    RETRYABLE_ERROR_CODES,
    CLIENT_ERROR_CODES,
    getHttpStatus,
    isRetryableError,
    isClientError,
    createErrorResponse,

    // ========================================================
    // HTTP status
    // ========================================================
    HTTP_STATUS,
    HTTP_STATUS_CATEGORY,
    HTTP_STATUS_MESSAGE,
    getStatusMessage,
    isHttpSuccess,
    isHttpClientError,
    isServerError,
    isRetryableStatus,

    // ========================================================
    // Config
    // ========================================================
    TIMEOUTS,
    RETRY_CONFIG,
    RATE_LIMITS,
    CIRCUIT_BREAKER_CONFIG,
    HEALTH_CONFIG,
    LOG_CONFIG,
    METRICS_CONFIG,
    TRACING_CONFIG,
    CACHE_CONFIG,
    QUEUE_CONFIG,
    PAYMENT_CONFIG,
    REFUND_CONFIG,
    FEATURE_FLAGS,
    getEnvConfig,

    // ========================================================
    // Order
    // ========================================================
    ORDER_STATUS,
    ORDER_TRANSITIONS,

    // ========================================================
    // Anomaly
    // ========================================================
    ANOMALY_TYPES,
    ANOMALY_SEVERITY,

    // ========================================================
    // Risk
    // ========================================================
    RISK_LEVELS,

    // ========================================================
    // Tier (CRITICAL - Fixes paymentModel error)
    // ========================================================
    TIER_LEVELS,

    // ========================================================
    // Archive
    // ========================================================
    ARCHIVE_TIERS,

    // ========================================================
    // Transaction
    // ========================================================
    TRANSACTION_STATUS,
    TRANSACTION_TYPE,

    // ========================================================
    // Refund
    // ========================================================
    REFUND_STATUS,
    REFUND_TYPE,

    // ========================================================
    // Webhook
    // ========================================================
    WEBHOOK_EVENTS,
    WEBHOOK_PRIORITY,
    WEBHOOK_STATUS,

    // ========================================================
    // Rate limiting
    // ========================================================
    RATE_LIMIT_TYPES,

    // ========================================================
    // IP blocking
    // ========================================================
    IP_BLOCK_REASONS,

    // ========================================================
    // Auth
    // ========================================================
    AUTH_ERRORS,

    // ========================================================
    // Idempotency
    // ========================================================
    IDEMPOTENCY_STATUS,
    LOCK_STATES,

    // ========================================================
    // Circuit breaker
    // ========================================================
    CIRCUIT_STATES,

    // ========================================================
    // Health
    // ========================================================
    HEALTH_STATUS,

    // ========================================================
    // Sort options
    // ========================================================
    SORT_OPTIONS,
};