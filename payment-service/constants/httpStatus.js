/**
 * ============================================================
 * 🌐 HTTP STATUS CONSTANTS — STANDARD RESPONSE CODES
 * ============================================================
 *
 * PURPOSE:
 * - Centralized HTTP status code definitions
 * - Status code categories and descriptions
 *
 * SCALE TARGET:
 * - Consistent API responses
 *
 * ============================================================
 */

/**
 * HTTP Status Codes
 */
const HTTP_STATUS = {
    // 1xx Informational
    CONTINUE: 100,
    SWITCHING_PROTOCOLS: 101,
    PROCESSING: 102,

    // 2xx Success
    OK: 200,
    CREATED: 201,
    ACCEPTED: 202,
    NON_AUTHORITATIVE_INFORMATION: 203,
    NO_CONTENT: 204,
    RESET_CONTENT: 205,
    PARTIAL_CONTENT: 206,

    // 3xx Redirection
    MULTIPLE_CHOICES: 300,
    MOVED_PERMANENTLY: 301,
    FOUND: 302,
    SEE_OTHER: 303,
    NOT_MODIFIED: 304,
    USE_PROXY: 305,
    TEMPORARY_REDIRECT: 307,
    PERMANENT_REDIRECT: 308,

    // 4xx Client Errors
    BAD_REQUEST: 400,
    UNAUTHORIZED: 401,
    PAYMENT_REQUIRED: 402,
    FORBIDDEN: 403,
    NOT_FOUND: 404,
    METHOD_NOT_ALLOWED: 405,
    NOT_ACCEPTABLE: 406,
    PROXY_AUTHENTICATION_REQUIRED: 407,
    REQUEST_TIMEOUT: 408,
    CONFLICT: 409,
    GONE: 410,
    LENGTH_REQUIRED: 411,
    PRECONDITION_FAILED: 412,
    PAYLOAD_TOO_LARGE: 413,
    URI_TOO_LONG: 414,
    UNSUPPORTED_MEDIA_TYPE: 415,
    RANGE_NOT_SATISFIABLE: 416,
    EXPECTATION_FAILED: 417,
    IM_A_TEAPOT: 418,
    UNPROCESSABLE_ENTITY: 422,
    LOCKED: 423,
    FAILED_DEPENDENCY: 424,
    TOO_EARLY: 425,
    UPGRADE_REQUIRED: 426,
    PRECONDITION_REQUIRED: 428,
    TOO_MANY_REQUESTS: 429,
    REQUEST_HEADER_FIELDS_TOO_LARGE: 431,
    UNAVAILABLE_FOR_LEGAL_REASONS: 451,

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR: 500,
    NOT_IMPLEMENTED: 501,
    BAD_GATEWAY: 502,
    SERVICE_UNAVAILABLE: 503,
    GATEWAY_TIMEOUT: 504,
    HTTP_VERSION_NOT_SUPPORTED: 505,
    VARIANT_ALSO_NEGOTIATES: 506,
    INSUFFICIENT_STORAGE: 507,
    LOOP_DETECTED: 508,
    NOT_EXTENDED: 510,
    NETWORK_AUTHENTICATION_REQUIRED: 511,
};

/**
 * Status code categories
 */
const HTTP_STATUS_CATEGORY = {
    INFORMATIONAL: (code) => code >= 100 && code < 200,
    SUCCESS: (code) => code >= 200 && code < 300,
    REDIRECTION: (code) => code >= 300 && code < 400,
    CLIENT_ERROR: (code) => code >= 400 && code < 500,
    SERVER_ERROR: (code) => code >= 500 && code < 600,
};

/**
 * Human-readable status messages
 */
const HTTP_STATUS_MESSAGE = {
    [HTTP_STATUS.OK]: 'OK',
    [HTTP_STATUS.CREATED]: 'Created',
    [HTTP_STATUS.ACCEPTED]: 'Accepted',
    [HTTP_STATUS.NO_CONTENT]: 'No Content',
    [HTTP_STATUS.BAD_REQUEST]: 'Bad Request',
    [HTTP_STATUS.UNAUTHORIZED]: 'Unauthorized',
    [HTTP_STATUS.PAYMENT_REQUIRED]: 'Payment Required',
    [HTTP_STATUS.FORBIDDEN]: 'Forbidden',
    [HTTP_STATUS.NOT_FOUND]: 'Not Found',
    [HTTP_STATUS.METHOD_NOT_ALLOWED]: 'Method Not Allowed',
    [HTTP_STATUS.CONFLICT]: 'Conflict',
    [HTTP_STATUS.TOO_MANY_REQUESTS]: 'Too Many Requests',
    [HTTP_STATUS.INTERNAL_SERVER_ERROR]: 'Internal Server Error',
    [HTTP_STATUS.SERVICE_UNAVAILABLE]: 'Service Unavailable',
    [HTTP_STATUS.GATEWAY_TIMEOUT]: 'Gateway Timeout',
    [HTTP_STATUS.REQUEST_TIMEOUT]: 'Request Timeout',
};

/**
 * Get status message
 */
const getStatusMessage = (statusCode) => {
    return HTTP_STATUS_MESSAGE[statusCode] || 'Unknown Status';
};

/**
 * Check if status code indicates success
 */
const isSuccess = (statusCode) => {
    return HTTP_STATUS_CATEGORY.SUCCESS(statusCode);
};

/**
 * Check if status code indicates client error
 */
const isClientError = (statusCode) => {
    return HTTP_STATUS_CATEGORY.CLIENT_ERROR(statusCode);
};

/**
 * Check if status code indicates server error
 */
const isServerError = (statusCode) => {
    return HTTP_STATUS_CATEGORY.SERVER_ERROR(statusCode);
};

/**
 * Check if status code is retryable (server errors and rate limits)
 */
const isRetryableStatus = (statusCode) => {
    return statusCode === HTTP_STATUS.TOO_MANY_REQUESTS ||
        statusCode === HTTP_STATUS.INTERNAL_SERVER_ERROR ||
        statusCode === HTTP_STATUS.SERVICE_UNAVAILABLE ||
        statusCode === HTTP_STATUS.GATEWAY_TIMEOUT ||
        statusCode === HTTP_STATUS.REQUEST_TIMEOUT;
};

module.exports = {
    HTTP_STATUS,
    HTTP_STATUS_CATEGORY,
    HTTP_STATUS_MESSAGE,
    getStatusMessage,
    isSuccess,
    isClientError,
    isServerError,
    isRetryableStatus,
};