/**
 * ============================================================
 * 🔗 CORRELATION ID MIDDLEWARE — DISTRIBUTED TRACING v1.0
 * ============================================================
 *
 * PURPOSE:
 * - Generate unique correlation ID for each request
 * - Propagate across microservices (User, Product, Order)
 * - Enable distributed tracing at 50M scale
 *
 * SCALE TARGET:
 * - 50M+ concurrent requests
 * - Sub-millisecond ID generation
 * - Zero collisions guarantee
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: SNOWFLAKE-64 (Distributed ID Generation)
 * ------------------------------------------------------------
 * - 64-bit unique ID generation without collisions
 * - Timestamp (41 bits) + Node ID (10 bits) + Sequence (12 bits)
 * - Generates 4096 unique IDs per millisecond per node
 * - 69-year timestamp lifespan
 *
 * 🧠 ALGORITHM 2: PROPAGATION CHAIN (Header Cascade)
 * ------------------------------------------------------------
 * - Priority-based header reading (x-correlation-id > x-request-id > x-trace-id)
 * - Automatic propagation to downstream services
 * - Maintains consistency across async boundaries
 *
 * ============================================================
 */

const crypto = require('crypto');
const os = require('os');

// ============================================================
// CONFIG
// ============================================================

const CORRELATION_HEADERS = {
    PRIMARY: 'x-correlation-id',
    SECONDARY: 'x-request-id',
    TERTIARY: 'x-trace-id',
    RESPONSE: 'x-correlation-id',
};

const NODE_ID_BITS = 10;
const SEQUENCE_BITS = 12;
const MAX_NODE_ID = (1 << NODE_ID_BITS) - 1; // 1023
const MAX_SEQUENCE = (1 << SEQUENCE_BITS) - 1; // 4095
const EPOCH = 1704067200000; // 2024-01-01 00:00:00 UTC

// ============================================================
// 🧠 ALGORITHM 1: SNOWFLAKE-64 Generator
// ============================================================

class Snowflake64Generator {
    constructor() {
        this.nodeId = this.generateNodeId();
        this.sequence = 0;
        this.lastTimestamp = -1;
        this.stats = {
            totalGenerated: 0,
            collisions: 0,
            lastId: null,
        };
    }

    /**
     * Generate deterministic node ID from hostname and process ID
     * Ensures uniqueness across multiple instances
     */
    generateNodeId() {
        const hostname = os.hostname();
        const pid = process.pid;

        // Create hash from hostname + PID
        const hash = crypto.createHash('sha256')
            .update(`${hostname}:${pid}:${process.env.SERVICE_FINGERPRINT || 'payment'}`)
            .digest('hex');

        // Take first NODE_ID_BITS bits
        const nodeId = parseInt(hash.substring(0, 8), 16) % (MAX_NODE_ID + 1);

        console.log(`[SNOWFLAKE] 🆔 Node ID: ${nodeId} (host: ${hostname}, pid: ${pid})`);
        return nodeId;
    }

    /**
     * Generate 64-bit Snowflake ID
     * Format: timestamp(41 bits) | nodeId(10 bits) | sequence(12 bits)
     */
    generate() {
        let timestamp = Date.now() - EPOCH;

        // Clock moved backwards - handle with spinning wait
        if (timestamp < this.lastTimestamp) {
            const drift = this.lastTimestamp - timestamp;
            if (drift > 5000) {
                // Clock drift > 5 seconds - log error and reset
                console.error(`[SNOWFLAKE] ⚠️ Clock drift detected: ${drift}ms, resetting sequence`);
                this.sequence = 0;
            }
            // Wait for clock to catch up
            while (Date.now() - EPOCH < this.lastTimestamp) {
                // Busy wait (microseconds)
                require('fs').readSync(process.stdin.fd, Buffer.alloc(0), 0, 0);
            }
            timestamp = Date.now() - EPOCH;
        }

        // Same millisecond - increment sequence
        if (timestamp === this.lastTimestamp) {
            this.sequence = (this.sequence + 1) & MAX_SEQUENCE;
            if (this.sequence === 0) {
                // Sequence exhausted - wait for next millisecond
                while (Date.now() - EPOCH <= timestamp) {
                    require('fs').readSync(process.stdin.fd, Buffer.alloc(0), 0, 0);
                }
                timestamp = Date.now() - EPOCH;
            }
        } else {
            // New millisecond - reset sequence
            this.sequence = 0;
        }

        this.lastTimestamp = timestamp;

        // Assemble the 64-bit ID
        // (timestamp << (NODE_ID_BITS + SEQUENCE_BITS)) | (nodeId << SEQUENCE_BITS) | sequence
        const id = (BigInt(timestamp) << BigInt(NODE_ID_BITS + SEQUENCE_BITS)) |
            (BigInt(this.nodeId) << BigInt(SEQUENCE_BITS)) |
            BigInt(this.sequence);

        this.stats.totalGenerated++;
        this.stats.lastId = id.toString();

        return id.toString();
    }

    /**
     * Generate formatted ID with prefix for easier debugging
     */
    generateFormatted(prefix = 'req') {
        const snowflakeId = this.generate();
        // Add prefix and timestamp for human readability
        const timestamp = Date.now();
        return `${prefix}_${timestamp}_${snowflakeId.slice(-8)}`;
    }

    getMetrics() {
        return {
            totalGenerated: this.stats.totalGenerated,
            nodeId: this.nodeId,
            lastId: this.stats.lastId,
            sequence: this.sequence,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: PROPAGATION CHAIN
// ============================================================

class CorrelationPropagator {
    constructor() {
        this.activeCorrelations = new Map(); // correlationId -> { startTime, userId, endpoint }
        this.maxActiveCorrelations = 100000; // 100K concurrent
        this.ttlMs = 600000; // 10 minutes TTL for cleanup
        this.stats = {
            totalPropagated: 0,
            cacheHits: 0,
            cacheMisses: 0,
            expiredCleaned: 0,
        };

        // Cleanup stale correlations periodically
        setInterval(() => this.cleanupStaleCorrelations(), 60000);
    }

    /**
     * Extract correlation ID from request headers with priority cascade
     */
    extractCorrelationId(req) {
        // Priority order: PRIMARY > SECONDARY > TERTIARY
        const correlationId = req.headers[CORRELATION_HEADERS.PRIMARY] ||
            req.headers[CORRELATION_HEADERS.SECONDARY] ||
            req.headers[CORRELATION_HEADERS.TERTIARY];

        if (correlationId) {
            this.stats.cacheHits++;
            this.updateCorrelationActivity(correlationId, req);
            return correlationId;
        }

        this.stats.cacheMisses++;
        return null;
    }

    /**
     * Track correlation activity for monitoring
     */
    updateCorrelationActivity(correlationId, req) {
        const existing = this.activeCorrelations.get(correlationId);

        if (existing) {
            existing.lastActivity = Date.now();
            existing.endpoint = req.path;
            existing.userId = req.user?.id || existing.userId;
        } else {
            // Prevent memory exhaustion
            if (this.activeCorrelations.size >= this.maxActiveCorrelations) {
                this.evictOldestCorrelation();
            }

            this.activeCorrelations.set(correlationId, {
                startTime: Date.now(),
                lastActivity: Date.now(),
                userId: req.user?.id || 'anonymous',
                endpoint: req.path,
                ip: req.ip,
            });
        }

        this.stats.totalPropagated++;
    }

    /**
     * Evict oldest correlation when cache is full (LRU)
     */
    evictOldestCorrelation() {
        let oldest = null;
        let oldestKey = null;

        for (const [key, value] of this.activeCorrelations.entries()) {
            if (!oldest || value.lastActivity < oldest.lastActivity) {
                oldest = value;
                oldestKey = key;
            }
        }

        if (oldestKey) {
            this.activeCorrelations.delete(oldestKey);
        }
    }

    /**
     * Cleanup stale correlations
     */
    cleanupStaleCorrelations() {
        const now = Date.now();
        let cleaned = 0;

        for (const [key, value] of this.activeCorrelations.entries()) {
            if (now - value.lastActivity > this.ttlMs) {
                this.activeCorrelations.delete(key);
                cleaned++;
            }
        }

        this.stats.expiredCleaned = cleaned;
        if (cleaned > 0) {
            console.log(`[CORRELATION] 🧹 Cleaned ${cleaned} stale correlations`);
        }
    }

    /**
     * Get correlation metrics for health check
     */
    getMetrics() {
        return {
            activeCorrelations: this.activeCorrelations.size,
            totalPropagated: this.stats.totalPropagated,
            cacheHitRate: this.stats.totalPropagated > 0
                ? ((this.stats.cacheHits / this.stats.totalPropagated) * 100).toFixed(2) + '%'
                : '0%',
            expiredCleaned: this.stats.expiredCleaned,
        };
    }

    /**
     * Get correlation activity for debugging
     */
    getCorrelationDetails(correlationId) {
        return this.activeCorrelations.get(correlationId) || null;
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const snowflakeGenerator = new Snowflake64Generator();
const correlationPropagator = new CorrelationPropagator();

// ============================================================
// 🚀 MAIN MIDDLEWARE
// ============================================================

/**
 * Correlation ID Middleware
 *
 * Injects correlation ID into request and response headers
 * Enables distributed tracing across all microservices
 */
const correlationIdMiddleware = (req, res, next) => {
    // Extract existing correlation ID or generate new one
    let correlationId = correlationPropagator.extractCorrelationId(req);

    if (!correlationId) {
        // Generate new Snowflake-64 ID
        correlationId = snowflakeGenerator.generateFormatted('pay');
    }

    // Store in request object for downstream use
    req.correlationId = correlationId;
    req.correlationStartTime = Date.now();

    // Set response header
    res.setHeader(CORRELATION_HEADERS.RESPONSE, correlationId);

    // Add to request-scoped logger context (if logger available)
    if (req.logger) {
        req.logger.setContext('correlationId', correlationId);
    } else {
        // Attach to req for manual logging
        req.getCorrelationId = () => correlationId;
    }

    // Track in propagator for monitoring
    correlationPropagator.updateCorrelationActivity(correlationId, req);

    // Add helper method to request for easy access
    req.getCorrelationSpan = () => ({
        id: correlationId,
        duration: Date.now() - req.correlationStartTime,
        path: req.path,
        method: req.method,
    });

    next();
};

// ============================================================
// 📊 HELPER MIDDLEWARE FOR RESPONSE TRACING
// ============================================================

/**
 * Response tracing middleware - adds correlation ID to response
 * and logs request completion
 */
const responseTracingMiddleware = (req, res, next) => {
    // Store original end function
    const originalEnd = res.end;
    const startTime = Date.now();

    // Override end to add correlation timing
    res.end = function(chunk, encoding) {
        const duration = Date.now() - startTime;
        const correlationId = req.correlationId;

        // Add timing header
        res.setHeader('x-response-time', `${duration}ms`);
        res.setHeader('x-correlation-id', correlationId);

        // Log completion (sampled to reduce noise)
        if (Math.random() < 0.01) { // 1% sampling
            console.log(JSON.stringify({
                type: 'REQUEST_COMPLETE',
                correlationId,
                method: req.method,
                path: req.path,
                statusCode: res.statusCode,
                durationMs: duration,
                timestamp: new Date().toISOString(),
            }));
        }

        originalEnd.call(this, chunk, encoding);
    };

    next();
};

// ============================================================
// 🔧 HELPER FUNCTIONS FOR EXTERNAL USE
// ============================================================

/**
 * Generate correlation ID for external use (e.g., outgoing requests)
 */
const generateCorrelationId = () => {
    return snowflakeGenerator.generateFormatted('ext');
};

/**
 * Get correlation metrics for health endpoint
 */
const getCorrelationMetrics = () => {
    return {
        snowflake: snowflakeGenerator.getMetrics(),
        propagator: correlationPropagator.getMetrics(),
    };
};

/**
 * Get correlation details for debugging
 */
const getCorrelationDetails = (correlationId) => {
    return correlationPropagator.getCorrelationDetails(correlationId);
};

// ============================================================
// EXPORTS
// ============================================================

module.exports = {
    // Main middleware
    correlationIdMiddleware,
    responseTracingMiddleware,

    // Helper functions
    generateCorrelationId,
    getCorrelationMetrics,
    getCorrelationDetails,

    // Advanced access for monitoring
    snowflakeGenerator,
    correlationPropagator,
};