/**
 * ============================================================
 * 🛡️ SECURITY MIDDLEWARE — ZERO-TRUST EDGE PROTECTION v1.0
 * ============================================================
 *
 * PURPOSE:
 * - Protect payment service from common web vulnerabilities
 * - Implement defense-in-depth security layers
 * - Prevent XSS, CSRF, SQL injection, clickjacking, MIME sniffing
 *
 * SCALE TARGET:
 * - 50M+ concurrent requests
 * - Sub-millisecond security checks
 * - Zero false positives
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: ADAPTIVE CSP (Content Security Policy) Generator
 * ------------------------------------------------------------
 * - Dynamically generates CSP headers based on request context
 * - Detects script injection attempts in real-time
 * - Auto-blocks malicious patterns with nonce rotation
 *
 * 🧠 ALGORITHM 2: REQUEST SIGNATURE VALIDATION (Anti-Replay)
 * ------------------------------------------------------------
 * - Validates request signatures for idempotent operations
 * - Prevents replay attacks on payment endpoints
 * - Time-windowed signature verification (5 second tolerance)
 *
 * ============================================================
 */

const helmet = require('helmet');
const crypto = require('crypto');

// ============================================================
// CONFIG
// ============================================================

const IS_PRODUCTION = process.env.NODE_ENV === 'production';
const TRUSTED_DOMAINS = (process.env.TRUSTED_DOMAINS || '*.purehomes.com,*.stripe.com').split(',');
const REQUEST_SIGNATURE_ENABLED = process.env.REQUEST_SIGNING_ENABLED === 'true';
const SIGNATURE_TTL_MS = 5000; // 5 seconds window

// ============================================================
// 🧠 ALGORITHM 1: ADAPTIVE CSP GENERATOR
// ============================================================

class AdaptiveCSPGenerator {
    constructor() {
        this.nonceCache = new Map(); // nonce -> { expiresAt, requestCount }
        this.nonceTTLMs = 60000; // 1 minute
        this.suspiciousPatterns = [
            /<script/i,
            /javascript:/i,
            /onerror=/i,
            /onload=/i,
            /eval\(/i,
            /expression\(/i,
        ];
        this.stats = {
            noncesGenerated: 0,
            scriptBlocks: 0,
            cspViolations: 0,
        };

        // Cleanup expired nonces
        setInterval(() => this.cleanupNonces(), 60000);
    }

    /**
     * Generate cryptographically secure nonce
     */
    generateNonce() {
        const nonce = crypto.randomBytes(32).toString('base64');
        this.nonceCache.set(nonce, {
            expiresAt: Date.now() + this.nonceTTLMs,
            requestCount: 0,
        });
        this.stats.noncesGenerated++;
        return nonce;
    }

    /**
     * Validate and consume nonce
     */
    validateNonce(nonce) {
        const cached = this.nonceCache.get(nonce);
        if (!cached) return false;

        if (cached.expiresAt < Date.now()) {
            this.nonceCache.delete(nonce);
            return false;
        }

        // Limit nonce reuse (max 5 requests per nonce)
        if (cached.requestCount >= 5) {
            this.stats.scriptBlocks++;
            return false;
        }

        cached.requestCount++;
        this.nonceCache.set(nonce, cached);
        return true;
    }

    /**
     * Detect script injection in request
     */
    detectScriptInjection(req) {
        const checkString = (str) => {
            if (!str) return false;
            for (const pattern of this.suspiciousPatterns) {
                if (pattern.test(str)) {
                    this.stats.cspViolations++;
                    return true;
                }
            }
            return false;
        };

        // Check query parameters
        for (const value of Object.values(req.query)) {
            if (checkString(String(value))) return true;
        }

        // Check body (if string)
        if (req.body && typeof req.body === 'object') {
            for (const value of Object.values(req.body)) {
                if (typeof value === 'string' && checkString(value)) return true;
            }
        }

        return false;
    }

    /**
     * Generate CSP policy based on request context
     */
    generateCSP(req, nonce) {
        const isPaymentEndpoint = req.path.includes('/payment') || req.path.includes('/refund');
        const isWebhookEndpoint = req.path.includes('/webhook');

        // Base CSP policy
        const policies = [
            "default-src 'self'",
            "script-src 'self' 'nonce-${nonce}' 'strict-dynamic'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: https:",
            "font-src 'self'",
            "connect-src 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "form-action 'self'",
        ];

        // Payment endpoints need Stripe
        if (isPaymentEndpoint) {
            policies.push("script-src 'self' 'nonce-${nonce}' https://js.stripe.com https://*.stripe.com");
            policies.push("connect-src 'self' https://api.stripe.com");
            policies.push("frame-src https://js.stripe.com https://*.stripe.com");
        }

        // Webhook endpoints are more restrictive
        if (isWebhookEndpoint) {
            policies.push("script-src 'none'");
            policies.push("frame-ancestors 'none'");
        }

        // Add reporting for production
        if (IS_PRODUCTION) {
            policies.push("report-uri /api/csp-report");
        }

        return policies.join('; ');
    }

    /**
     * Cleanup expired nonces
     */
    cleanupNonces() {
        const now = Date.now();
        let cleaned = 0;

        for (const [nonce, data] of this.nonceCache.entries()) {
            if (data.expiresAt < now) {
                this.nonceCache.delete(nonce);
                cleaned++;
            }
        }

        if (cleaned > 0) {
            console.log(`[CSP] 🧹 Cleaned ${cleaned} expired nonces`);
        }
    }

    getMetrics() {
        return {
            noncesGenerated: this.stats.noncesGenerated,
            scriptBlocks: this.stats.scriptBlocks,
            cspViolations: this.stats.cspViolations,
            activeNonces: this.nonceCache.size,
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: REQUEST SIGNATURE VALIDATION
// ============================================================

class RequestSignatureValidator {
    constructor() {
        this.signatureCache = new Map(); // signatureHash -> { expiresAt, used }
        this.secretKey = process.env.REQUEST_SIGNING_KEY || crypto.randomBytes(32).toString('hex');
        this.stats = {
            totalSignatures: 0,
            validSignatures: 0,
            invalidSignatures: 0,
            replayAttacks: 0,
        };

        // Cleanup expired signatures
        setInterval(() => this.cleanupSignatures(), 60000);
    }

    /**
     * Generate request signature
     */
    generateSignature(method, path, body, timestamp, secret = null) {
        const key = secret || this.secretKey;
        const bodyHash = body ? crypto.createHash('sha256').update(JSON.stringify(body)).digest('hex') : '';
        const stringToSign = `${method}|${path}|${bodyHash}|${timestamp}`;

        return crypto.createHmac('sha256', key)
            .update(stringToSign)
            .digest('hex');
    }

    /**
     * Validate request signature (prevents replay attacks)
     */
    validateSignature(req, providedSignature) {
        this.stats.totalSignatures++;

        const timestamp = parseInt(req.headers['x-request-timestamp']);
        const method = req.method;
        const path = req.originalUrl || req.url;
        const body = req.body;

        // Validate timestamp (prevent replay with wrong time)
        if (!timestamp || Math.abs(Date.now() - timestamp) > SIGNATURE_TTL_MS) {
            this.stats.invalidSignatures++;
            return { valid: false, reason: 'INVALID_TIMESTAMP' };
        }

        // Generate expected signature
        const expectedSignature = this.generateSignature(method, path, body, timestamp);

        // Time-safe comparison
        const isValid = crypto.timingSafeEqual(
            Buffer.from(providedSignature || ''),
            Buffer.from(expectedSignature)
        );

        if (!isValid) {
            this.stats.invalidSignatures++;
            return { valid: false, reason: 'SIGNATURE_MISMATCH' };
        }

        // Check for replay attack (signature already used)
        const signatureHash = crypto.createHash('sha256')
            .update(providedSignature)
            .digest('hex');

        if (this.signatureCache.has(signatureHash)) {
            this.stats.replayAttacks++;
            this.stats.invalidSignatures++;
            return { valid: false, reason: 'REPLAY_ATTACK' };
        }

        // Store used signature
        this.signatureCache.set(signatureHash, {
            expiresAt: Date.now() + SIGNATURE_TTL_MS,
            usedAt: Date.now(),
        });

        this.stats.validSignatures++;
        return { valid: true };
    }

    /**
     * Cleanup expired signatures
     */
    cleanupSignatures() {
        const now = Date.now();
        let cleaned = 0;

        for (const [sig, data] of this.signatureCache.entries()) {
            if (data.expiresAt < now) {
                this.signatureCache.delete(sig);
                cleaned++;
            }
        }

        if (cleaned > 0) {
            console.log(`[SIGNATURE] 🧹 Cleaned ${cleaned} expired signatures`);
        }
    }

    getMetrics() {
        const total = this.stats.totalSignatures;
        return {
            totalSignatures: total,
            validRate: total > 0 ? ((this.stats.validSignatures / total) * 100).toFixed(2) + '%' : '0%',
            validSignatures: this.stats.validSignatures,
            invalidSignatures: this.stats.invalidSignatures,
            replayAttacks: this.stats.replayAttacks,
            cacheSize: this.signatureCache.size,
        };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const cspGenerator = new AdaptiveCSPGenerator();
const signatureValidator = new RequestSignatureValidator();

// ============================================================
// 🚀 SECURITY MIDDLEWARE
// ============================================================

/**
 * Main security middleware combining all protections
 */
const securityMiddleware = (req, res, next) => {
    // Generate nonce for this request
    const nonce = cspGenerator.generateNonce();
    req.nonce = nonce;

    // Detect script injection before processing
    if (cspGenerator.detectScriptInjection(req)) {
        console.warn(`[SECURITY] 🚨 Script injection detected: ${req.ip} -> ${req.path}`);
        return res.status(403).json({
            success: false,
            message: 'Potential security threat detected',
            code: 'SECURITY_BLOCK',
        });
    }

    // Apply Helmet security headers
    helmet({
        contentSecurityPolicy: {
            directives: {
                defaultSrc: ["'self'"],
                scriptSrc: [
                    "'self'",
                    `'nonce-${nonce}'`,
                    "'strict-dynamic'",
                    ...(req.path.includes('/payment') ? ['https://js.stripe.com'] : []),
                ],
                styleSrc: ["'self'", "'unsafe-inline'"],
                imgSrc: ["'self'", 'data:', 'https:'],
                connectSrc: ["'self'", ...(req.path.includes('/payment') ? ['https://api.stripe.com'] : [])],
                frameSrc: req.path.includes('/payment') ? ['https://js.stripe.com'] : ["'none'"],
                frameAncestors: ["'none'"],
                baseUri: ["'self'"],
                formAction: ["'self'"],
                upgradeInsecureRequests: IS_PRODUCTION ? [] : null,
            },
            reportOnly: !IS_PRODUCTION,
        },
        hsts: {
            maxAge: 31536000,
            includeSubDomains: true,
            preload: true,
        },
        frameguard: { action: 'deny' },
        noSniff: true,
        xssFilter: true,
        hidePoweredBy: true,
        referrerPolicy: { policy: 'strict-origin-when-cross-origin' },
    })(req, res, () => {});

    // Set additional security headers
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    res.setHeader('Permissions-Policy', 'geolocation=(), microphone=(), camera=()');
    res.setHeader('Cross-Origin-Embedder-Policy', 'require-corp');
    res.setHeader('Cross-Origin-Opener-Policy', 'same-origin');
    res.setHeader('Cross-Origin-Resource-Policy', 'same-origin');

    next();
};

// ============================================================
// 🚀 REQUEST SIGNATURE MIDDLEWARE (For idempotent endpoints)
// ============================================================

/**
 * Request signature validation middleware
 * Use on payment, refund, and other idempotent endpoints
 */
const requestSignatureMiddleware = (req, res, next) => {
    if (!REQUEST_SIGNATURE_ENABLED) {
        return next();
    }

    const signature = req.headers['x-request-signature'];

    if (!signature) {
        return res.status(401).json({
            success: false,
            message: 'Missing request signature',
            code: 'SIGNATURE_REQUIRED',
        });
    }

    const validation = signatureValidator.validateSignature(req, signature);

    if (!validation.valid) {
        console.warn(`[SECURITY] 🚨 Invalid signature: ${validation.reason} from ${req.ip}`);
        return res.status(401).json({
            success: false,
            message: 'Invalid request signature',
            code: 'INVALID_SIGNATURE',
            reason: validation.reason,
        });
    }

    next();
};

// ============================================================
// 🚀 RATE LIMIT HEADER MIDDLEWARE
// ============================================================

/**
 * Adds rate limit headers to response
 */
const rateLimitHeadersMiddleware = (req, res, next) => {
    // Store original send function
    const originalJson = res.json;

    res.json = function(body) {
        // Add rate limit headers if available from rate limiter
        if (req.rateLimit) {
            res.setHeader('X-RateLimit-Limit', req.rateLimit.limit);
            res.setHeader('X-RateLimit-Remaining', req.rateLimit.remaining);
            res.setHeader('X-RateLimit-Reset', req.rateLimit.reset);
        }

        // Add security headers
        res.setHeader('X-Content-Type-Options', 'nosniff');
        res.setHeader('X-Frame-Options', 'DENY');

        originalJson.call(this, body);
    };

    next();
};

// ============================================================
// 🚀 CSP REPORT ENDPOINT HANDLER
// ============================================================

/**
 * Handle CSP violation reports
 */
const cspReportHandler = (req, res) => {
    const report = req.body;

    console.warn('[CSP] Violation report:', JSON.stringify({
        blockedURI: report['csp-report']?.['blocked-uri'],
        violatedDirective: report['csp-report']?.['violated-directive'],
        documentURI: report['csp-report']?.['document-uri'],
        timestamp: new Date().toISOString(),
    }));

    cspGenerator.stats.cspViolations++;

    res.status(204).end();
};

// ============================================================
// 📊 METRICS & HEALTH CHECK
// ============================================================

const getSecurityMetrics = () => {
    return {
        csp: cspGenerator.getMetrics(),
        signature: signatureValidator.getMetrics(),
        timestamp: new Date().toISOString(),
    };
};

const securityHealthCheck = () => {
    const cspMetrics = cspGenerator.getMetrics();
    const signatureMetrics = signatureValidator.getMetrics();

    let status = 'HEALTHY';

    if (signatureMetrics.replayAttacks > 100) {
        status = 'ATTACK_DETECTED';
    }

    if (cspMetrics.cspViolations > 1000) {
        status = 'DEGRADED';
    }

    return {
        status,
        timestamp: new Date().toISOString(),
        metrics: {
            cspViolations: cspMetrics.cspViolations,
            replayAttacks: signatureMetrics.replayAttacks,
            validSignatureRate: signatureMetrics.validRate,
        },
    };
};

// ============================================================
// 🧠 INNOVATION: SQL/NoSQL Injection Prevention
// ============================================================

const INJECTION_PATTERNS = [
    /\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|CREATE)\b/i,
    /\b(OR|AND)\s+\d+\s*=\s*\d+/i,
    /\b(WHERE)\s+\d+\s*=\s*\d+/i,
    /\b(SCRIPT|ALERT|PROMPT|CONFIRM)\b/i,
    /\b(SLEEP|BENCHMARK|WAITFOR)\s*\(/i,
    /\b(EXEC|EXECUTE|XP_CMDSHELL|SP_EXECUTESQL)\b/i,
];

/**
 * Detect injection attempts in request
 */
const detectInjection = (value) => {
    if (!value || typeof value !== 'string') return false;

    for (const pattern of INJECTION_PATTERNS) {
        if (pattern.test(value)) {
            return true;
        }
    }
    return false;
};

/**
 * Injection prevention middleware
 */
const injectionPreventionMiddleware = (req, res, next) => {
    // Check query parameters
    for (const [key, value] of Object.entries(req.query)) {
        if (detectInjection(String(value))) {
            console.warn(`[SECURITY] 🚨 Injection attempt: ${key}=${value} from ${req.ip}`);
            return res.status(403).json({
                success: false,
                message: 'Invalid request parameters',
                code: 'INJECTION_DETECTED',
            });
        }
    }

    // Check body
    if (req.body && typeof req.body === 'object') {
        const checkObject = (obj) => {
            for (const [key, value] of Object.entries(obj)) {
                if (typeof value === 'string' && detectInjection(value)) {
                    return true;
                }
                if (typeof value === 'object' && value !== null) {
                    if (checkObject(value)) return true;
                }
            }
            return false;
        };

        if (checkObject(req.body)) {
            console.warn(`[SECURITY] 🚨 Injection attempt in body from ${req.ip}`);
            return res.status(403).json({
                success: false,
                message: 'Invalid request body',
                code: 'INJECTION_DETECTED',
            });
        }
    }

    next();
};

// ============================================================
// EXPORTS
// ============================================================

module.exports = {
    // Main middleware
    securityMiddleware,
    requestSignatureMiddleware,
    rateLimitHeadersMiddleware,
    injectionPreventionMiddleware,

    // CSP report handler
    cspReportHandler,

    // Metrics and health
    getSecurityMetrics,
    securityHealthCheck,

    // Individual components for advanced use
    cspGenerator,
    signatureValidator,
};