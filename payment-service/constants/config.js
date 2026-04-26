/**
 * ============================================================
 * ⚙️ CONFIG CONSTANTS — APPLICATION WIDE CONFIGURATION
 * ============================================================
 *
 * PURPOSE:
 * - Centralized configuration constants
 * - Timeouts, limits, retry policies
 * - Feature flags and defaults
 *
 * SCALE TARGET:
 * - 50M+ users
 * - Tunable parameters for performance
 *
 * ============================================================
 */

/**
 * Timeout constants (milliseconds)
 */
const TIMEOUTS = {
    // HTTP timeouts
    DEFAULT_API_TIMEOUT_MS: 30000,
    LONG_API_TIMEOUT_MS: 60000,
    SHORT_API_TIMEOUT_MS: 5000,

    // Database timeouts
    DB_CONNECTION_TIMEOUT_MS: 10000,
    DB_QUERY_TIMEOUT_MS: 30000,
    DB_TRANSACTION_TIMEOUT_MS: 60000,

    // External service timeouts
    STRIPE_API_TIMEOUT_MS: 10000,
    ORDER_SERVICE_TIMEOUT_MS: 8000,
    PRODUCT_SERVICE_TIMEOUT_MS: 8000,
    USER_SERVICE_TIMEOUT_MS: 5000,

    // Webhook timeouts
    WEBHOOK_PROCESSING_TIMEOUT_MS: 25000,
    WEBHOOK_RETRY_DELAY_MS: 1000,

    // Idempotency timeouts
    IDEMPOTENCY_LOCK_TTL_MS: 10000,
    IDEMPOTENCY_CACHE_TTL_MS: 86400000, // 24 hours

    // Circuit breaker timeouts
    CIRCUIT_BREAKER_TIMEOUT_MS: 30000,
    CIRCUIT_HALF_OPEN_TIMEOUT_MS: 15000,
};

/**
 * Retry configuration
 */
const RETRY_CONFIG = {
    MAX_RETRIES: 3,
    BASE_DELAY_MS: 100,
    MAX_DELAY_MS: 5000,
    BACKOFF_MULTIPLIER: 2,
    JITTER_MS: 50,
};

/**
 * Rate limiting configuration
 */
const RATE_LIMITS = {
    // Global limits
    GLOBAL_RPS: 6000,
    GLOBAL_RPM: 360000,

    // Per-user limits
    USER_RPS: 10,
    USER_RPM: 600,

    // Per-IP limits
    IP_RPS: 30,
    IP_RPM: 1800,

    // Endpoint-specific limits
    PAYMENT_CREATE_RPM: 30,
    PAYMENT_CONFIRM_RPM: 30,
    REFUND_RPM: 10,
    WEBHOOK_RPS: 100,
};

/**
 * Circuit breaker configuration
 */
const CIRCUIT_BREAKER_CONFIG = {
    FAILURE_THRESHOLD: 5,
    SUCCESS_THRESHOLD: 3,
    TIMEOUT_MS: TIMEOUTS.CIRCUIT_BREAKER_TIMEOUT_MS,
    HALF_OPEN_TIMEOUT_MS: TIMEOUTS.CIRCUIT_HALF_OPEN_TIMEOUT_MS,
    MONITOR_INTERVAL_MS: 10000,
};

/**
 * Health check configuration
 */
const HEALTH_CONFIG = {
    STARTUP_GRACE_PERIOD_MS: 30000,
    SHUTDOWN_GRACE_PERIOD_MS: 30000,
    HEALTH_CHECK_INTERVAL_MS: 30000,
    DEPENDENCY_TIMEOUT_MS: 5000,
};

/**
 * Logging configuration
 */
const LOG_CONFIG = {
    DEFAULT_LEVEL: 'info',
    PRODUCTION_LEVEL: 'info',
    DEVELOPMENT_LEVEL: 'debug',
    MAX_LOG_SIZE_MB: 100,
    MAX_LOG_FILES: 7,
    SAMPLING_RATE: 0.05,
};

/**
 * Metrics configuration
 */
const METRICS_CONFIG = {
    ENABLED: true,
    PORT: 9090,
    PATH: '/metrics',
    DEFAULT_BUCKETS: [0.01, 0.05, 0.1, 0.5, 1, 2, 5, 10, 30, 60],
    SLOW_REQUEST_THRESHOLD_MS: 1000,
    VERY_SLOW_REQUEST_THRESHOLD_MS: 3000,
};

/**
 * Tracing configuration
 */
const TRACING_CONFIG = {
    ENABLED: true,
    SAMPLE_RATE: 0.01,
    JAEGER_HOST: 'localhost',
    JAEGER_PORT: 6831,
    MAX_SPANS_PER_REQUEST: 100,
};

/**
 * Cache configuration
 */
const CACHE_CONFIG = {
    DEFAULT_TTL_SECONDS: 300,
    LONG_TTL_SECONDS: 3600,
    SHORT_TTL_SECONDS: 60,
    MAX_KEYS: 100000,
    REDIS_URL: process.env.REDIS_RATE_LIMIT_URL || 'redis://localhost:6379',
};

/**
 * Queue configuration
 */
const QUEUE_CONFIG = {
    MAX_SIZE: 10000,
    BATCH_SIZE: 100,
    PROCESSING_INTERVAL_MS: 100,
    BACKPRESSURE_THRESHOLD: 0.8,
    DEAD_LETTER_TTL_MS: 604800000, // 7 days
};

/**
 * Payment configuration
 */
const PAYMENT_CONFIG = {
    DEFAULT_CURRENCY: 'usd',
    MIN_AMOUNT: 0.5,
    MAX_AMOUNT: 1000000,
    ALLOWED_CURRENCIES: ['usd', 'eur', 'gbp', 'cad', 'aud', 'jpy'],
    STRIPE_API_VERSION: '2024-06-20',
};

/**
 * Refund configuration
 */
const REFUND_CONFIG = {
    MAX_REFUND_PERCENTAGE: 100,
    REFUND_WINDOW_DAYS: 90,
    AUTO_APPROVE_THRESHOLD: 100,
    MANUAL_REVIEW_THRESHOLD: 1000,
};

/**
 * Feature flags
 */
const FEATURE_FLAGS = {
    ENABLE_PAYMENT_PROCESSING: true,
    ENABLE_REFUNDS: true,
    ENABLE_WEBHOOKS: true,
    ENABLE_ASYNC_PROCESSING: true,
    ENABLE_CIRCUIT_BREAKER: true,
    ENABLE_DISTRIBUTED_TRACING: true,
    ENABLE_METRICS: true,
    ENABLE_RATE_LIMITING: true,
    ENABLE_IDEMPOTENCY_CACHE: true,
};

/**
 * Get environment-specific config
 */
const getEnvConfig = () => {
    const env = process.env.NODE_ENV || 'development';

    const configs = {
        development: {
            logLevel: LOG_CONFIG.DEVELOPMENT_LEVEL,
            metricsEnabled: true,
            tracingEnabled: true,
            rateLimitingEnabled: false,
        },
        staging: {
            logLevel: LOG_CONFIG.DEFAULT_LEVEL,
            metricsEnabled: true,
            tracingEnabled: true,
            rateLimitingEnabled: true,
        },
        production: {
            logLevel: LOG_CONFIG.PRODUCTION_LEVEL,
            metricsEnabled: true,
            tracingEnabled: true,
            rateLimitingEnabled: true,
        },
    };

    return configs[env] || configs.development;
};

module.exports = {
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
};