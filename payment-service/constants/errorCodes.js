/**
 * ============================================================
 * ⚠️ ERROR CODES CONSTANTS — STANDARDIZED ERROR RESPONSES
 * ============================================================
 *
 * PURPOSE:
 * - Centralized error code definitions
 * - HTTP status code mapping
 * - Retry decision logic
 *
 * SCALE TARGET:
 * - 50M+ error responses
 * - Consistent error handling
 *
 * ============================================================
 */

/**
 * Error code categories
 * Format: DOMAIN_CODE (e.g., PAY_001)
 */
const ERROR_CODES = {
    // ============================================================
    // Payment Domain (PAY_001 - PAY_099)
    // ============================================================
    PAYMENT_NOT_FOUND: 'PAY_001',
    PAYMENT_ALREADY_PROCESSED: 'PAY_002',
    PAYMENT_AMOUNT_MISMATCH: 'PAY_003',
    PAYMENT_CURRENCY_MISMATCH: 'PAY_004',
    PAYMENT_EXPIRED: 'PAY_005',
    PAYMENT_CANCELLATION_FAILED: 'PAY_006',
    PAYMENT_CONFIRMATION_FAILED: 'PAY_007',
    INVALID_PAYMENT_METHOD: 'PAY_008',
    PAYMENT_DECLINED: 'PAY_009',
    INSUFFICIENT_FUNDS: 'PAY_010',

    // ============================================================
    // Refund Domain (REF_001 - REF_099)
    // ============================================================
    REFUND_NOT_FOUND: 'REF_001',
    REFUND_AMOUNT_EXCEEDS_LIMIT: 'REF_002',
    REFUND_ALREADY_PROCESSED: 'REF_003',
    REFUND_FAILED: 'REF_004',
    INVALID_REFUND_AMOUNT: 'REF_005',
    REFUND_WINDOW_EXPIRED: 'REF_006',
    PARTIAL_REFUND_NOT_ALLOWED: 'REF_007',

    // ============================================================
    // Idempotency Domain (IDP_001 - IDP_099)
    // ============================================================
    DUPLICATE_REQUEST: 'IDP_001',
    IDEMPOTENCY_KEY_MISSING: 'IDP_002',
    IDEMPOTENCY_KEY_INVALID: 'IDP_003',
    IDEMPOTENCY_LOCK_TIMEOUT: 'IDP_004',
    IDEMPOTENCY_STORE_FAILURE: 'IDP_005',

    // ============================================================
    // Validation Domain (VAL_001 - VAL_099)
    // ============================================================
    MISSING_REQUIRED_FIELD: 'VAL_001',
    INVALID_FIELD_FORMAT: 'VAL_002',
    FIELD_VALUE_OUT_OF_RANGE: 'VAL_003',
    INVALID_EMAIL_FORMAT: 'VAL_004',
    INVALID_UUID_FORMAT: 'VAL_005',
    INVALID_AMOUNT: 'VAL_006',
    INVALID_CURRENCY: 'VAL_007',

    // ============================================================
    // Authentication Domain (AUTH_001 - AUTH_099)
    // ============================================================
    UNAUTHORIZED: 'AUTH_001',
    TOKEN_MISSING: 'AUTH_002',
    TOKEN_EXPIRED: 'AUTH_003',
    TOKEN_INVALID: 'AUTH_004',
    TOKEN_BLACKLISTED: 'AUTH_005',
    INSUFFICIENT_PERMISSIONS: 'AUTH_006',
    INVALID_CREDENTIALS: 'AUTH_007',

    // ============================================================
    // Rate Limiting Domain (RATE_001 - RATE_099)
    // ============================================================
    RATE_LIMIT_EXCEEDED: 'RATE_001',
    TOO_MANY_REQUESTS: 'RATE_002',
    BURST_LIMIT_EXCEEDED: 'RATE_003',
    CONCURRENT_LIMIT_EXCEEDED: 'RATE_004',

    // ============================================================
    // Circuit Breaker Domain (CB_001 - CB_099)
    // ============================================================
    CIRCUIT_OPEN: 'CB_001',
    SERVICE_DEGRADED: 'CB_002',
    SERVICE_UNAVAILABLE: 'CB_003',
    FALLBACK_EXECUTED: 'CB_004',

    // ============================================================
    // Stripe Domain (STR_001 - STR_099)
    // ============================================================
    STRIPE_API_ERROR: 'STR_001',
    STRIPE_AUTHENTICATION_FAILED: 'STR_002',
    STRIPE_INVALID_REQUEST: 'STR_003',
    STRIPE_RATE_LIMIT: 'STR_004',
    STRIPE_CONNECTION_ERROR: 'STR_005',

    // ============================================================
    // Database Domain (DB_001 - DB_099)
    // ============================================================
    DATABASE_CONNECTION_ERROR: 'DB_001',
    DATABASE_QUERY_TIMEOUT: 'DB_002',
    DATABASE_DUPLICATE_KEY: 'DB_003',
    DATABASE_TRANSACTION_FAILED: 'DB_004',

    // ============================================================
    // Webhook Domain (WH_001 - WH_099)
    // ============================================================
    WEBHOOK_SIGNATURE_INVALID: 'WH_001',
    WEBHOOK_TIMESTAMP_EXPIRED: 'WH_002',
    WEBHOOK_DELIVERY_FAILED: 'WH_003',
    WEBHOOK_PROCESSING_TIMEOUT: 'WH_004',

    // ============================================================
    // General Errors (GEN_001 - GEN_099)
    // ============================================================
    INTERNAL_SERVER_ERROR: 'GEN_001',
    SERVICE_UNAVAILABLE: 'GEN_002',
    REQUEST_TIMEOUT: 'GEN_003',
    BAD_REQUEST: 'GEN_004',
    NOT_FOUND: 'GEN_005',
    METHOD_NOT_ALLOWED: 'GEN_006',
    UNSUPPORTED_MEDIA_TYPE: 'GEN_007',
};

/**
 * HTTP status code mapping for each error code
 */
const ERROR_HTTP_STATUS = {
    [ERROR_CODES.PAYMENT_NOT_FOUND]: 404,
    [ERROR_CODES.PAYMENT_ALREADY_PROCESSED]: 409,
    [ERROR_CODES.PAYMENT_AMOUNT_MISMATCH]: 400,
    [ERROR_CODES.PAYMENT_DECLINED]: 402,
    [ERROR_CODES.INSUFFICIENT_FUNDS]: 402,
    [ERROR_CODES.REFUND_NOT_FOUND]: 404,
    [ERROR_CODES.REFUND_AMOUNT_EXCEEDS_LIMIT]: 400,
    [ERROR_CODES.REFUND_ALREADY_PROCESSED]: 409,
    [ERROR_CODES.DUPLICATE_REQUEST]: 409,
    [ERROR_CODES.IDEMPOTENCY_KEY_MISSING]: 400,
    [ERROR_CODES.MISSING_REQUIRED_FIELD]: 400,
    [ERROR_CODES.INVALID_FIELD_FORMAT]: 400,
    [ERROR_CODES.UNAUTHORIZED]: 401,
    [ERROR_CODES.TOKEN_MISSING]: 401,
    [ERROR_CODES.TOKEN_EXPIRED]: 401,
    [ERROR_CODES.TOKEN_INVALID]: 401,
    [ERROR_CODES.INSUFFICIENT_PERMISSIONS]: 403,
    [ERROR_CODES.RATE_LIMIT_EXCEEDED]: 429,
    [ERROR_CODES.TOO_MANY_REQUESTS]: 429,
    [ERROR_CODES.CIRCUIT_OPEN]: 503,
    [ERROR_CODES.SERVICE_DEGRADED]: 503,
    [ERROR_CODES.SERVICE_UNAVAILABLE]: 503,
    [ERROR_CODES.STRIPE_API_ERROR]: 502,
    [ERROR_CODES.DATABASE_CONNECTION_ERROR]: 503,
    [ERROR_CODES.WEBHOOK_SIGNATURE_INVALID]: 401,
    [ERROR_CODES.INTERNAL_SERVER_ERROR]: 500,
    [ERROR_CODES.REQUEST_TIMEOUT]: 408,
    [ERROR_CODES.BAD_REQUEST]: 400,
    [ERROR_CODES.NOT_FOUND]: 404,
};

/**
 * Whether an error is retryable
 */
const RETRYABLE_ERROR_CODES = [
    ERROR_CODES.CIRCUIT_OPEN,
    ERROR_CODES.SERVICE_DEGRADED,
    ERROR_CODES.SERVICE_UNAVAILABLE,
    ERROR_CODES.STRIPE_API_ERROR,
    ERROR_CODES.STRIPE_CONNECTION_ERROR,
    ERROR_CODES.DATABASE_CONNECTION_ERROR,
    ERROR_CODES.DATABASE_QUERY_TIMEOUT,
    ERROR_CODES.WEBHOOK_DELIVERY_FAILED,
    ERROR_CODES.REQUEST_TIMEOUT,
    ERROR_CODES.RATE_LIMIT_EXCEEDED,
    ERROR_CODES.TOO_MANY_REQUESTS,
];

/**
 * Whether an error is a client error (not retryable)
 */
const CLIENT_ERROR_CODES = [
    ERROR_CODES.PAYMENT_AMOUNT_MISMATCH,
    ERROR_CODES.PAYMENT_DECLINED,
    ERROR_CODES.INSUFFICIENT_FUNDS,
    ERROR_CODES.REFUND_AMOUNT_EXCEEDS_LIMIT,
    ERROR_CODES.DUPLICATE_REQUEST,
    ERROR_CODES.IDEMPOTENCY_KEY_MISSING,
    ERROR_CODES.MISSING_REQUIRED_FIELD,
    ERROR_CODES.INVALID_FIELD_FORMAT,
    ERROR_CODES.UNAUTHORIZED,
    ERROR_CODES.TOKEN_MISSING,
    ERROR_CODES.TOKEN_EXPIRED,
    ERROR_CODES.TOKEN_INVALID,
    ERROR_CODES.INSUFFICIENT_PERMISSIONS,
    ERROR_CODES.WEBHOOK_SIGNATURE_INVALID,
    ERROR_CODES.BAD_REQUEST,
];

/**
 * Get HTTP status code for error
 */
const getHttpStatus = (errorCode) => {
    return ERROR_HTTP_STATUS[errorCode] || 500;
};

/**
 * Check if error is retryable
 */
const isRetryableError = (errorCode) => {
    return RETRYABLE_ERROR_CODES.includes(errorCode);
};

/**
 * Check if error is client error
 */
const isClientError = (errorCode) => {
    return CLIENT_ERROR_CODES.includes(errorCode);
};

/**
 * Create error response object
 */
const createErrorResponse = (errorCode, message, details = null) => {
    return {
        success: false,
        code: errorCode,
        message,
        statusCode: getHttpStatus(errorCode),
        retryable: isRetryableError(errorCode),
        ...(details && { details }),
        timestamp: new Date().toISOString(),
    };
};

module.exports = {
    ERROR_CODES,
    ERROR_HTTP_STATUS,
    RETRYABLE_ERROR_CODES,
    CLIENT_ERROR_CODES,
    getHttpStatus,
    isRetryableError,
    isClientError,
    createErrorResponse,
};