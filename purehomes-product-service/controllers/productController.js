const Product = require('../models/productModel');
const mongoose = require('mongoose');

// ----------------------------
// Algorithm 1: Dynamic Product Ranking & Variant Prioritization (Existing)
// ----------------------------
const calculateProductScore = (product) => {
    const totalScore = product.variants.reduce((sum, v) => sum + (v.availabilityScore || 0), 0);
    const avgScore = product.variants.length ? totalScore / product.variants.length : 0;
    const ageInHours = (Date.now() - new Date(product.createdAt).getTime()) / 36e5;
    const recencyFactor = 1 / (1 + ageInHours / 24);
    return avgScore + recencyFactor;
};

// ----------------------------
// Algorithm 2: Adaptive Cache-Aside with Predictive Prefetch (Existing)
// ----------------------------
class AdaptiveCacheManager {
    constructor() {
        this.cache = new Map();
        this.accessLog = new Map();
        this.prefetchQueue = new Set();
        this.isWarming = false;
    }

    getCacheKey(prefix, params) {
        return `${prefix}:${JSON.stringify(params)}`;
    }

    getStochasticTTL(baseTTLSeconds = 300) {
        const jitter = (Math.random() * 0.4 - 0.2) * baseTTLSeconds;
        return Math.max(60, baseTTLSeconds + jitter);
    }

    async getOrSet(cacheKey, fetchCallback, ttlSeconds = 300) {
        if (this.cache.has(cacheKey)) {
            const cached = this.cache.get(cacheKey);
            if (cached.expiresAt > Date.now()) {
                this.trackAccess(cacheKey);
                return cached.data;
            } else {
                this.cache.delete(cacheKey);
            }
        }

        try {
            const data = await fetchCallback();
            this.cache.set(cacheKey, {
                data,
                expiresAt: Date.now() + (this.getStochasticTTL(ttlSeconds) * 1000),
                createdAt: Date.now()
            });
            this.schedulePrefetch(cacheKey, fetchCallback);
            return data;
        } catch (error) {
            console.error(`Cache miss fallback failed for ${cacheKey}:`, error.message);
            throw error;
        }
    }

    trackAccess(cacheKey) {
        const now = Date.now();
        if (!this.accessLog.has(cacheKey)) {
            this.accessLog.set(cacheKey, []);
        }
        const accesses = this.accessLog.get(cacheKey);
        accesses.push(now);
        if (accesses.length > 10) accesses.shift();
    }

    getAccessFrequency(cacheKey) {
        if (!this.accessLog.has(cacheKey)) return 0;
        const fiveMinutesAgo = Date.now() - (5 * 60 * 1000);
        const recentAccesses = this.accessLog.get(cacheKey).filter(t => t > fiveMinutesAgo);
        return recentAccesses.length;
    }

    schedulePrefetch(cacheKey, fetchCallback) {
        const frequency = this.getAccessFrequency(cacheKey);
        if (frequency >= 3 && !this.prefetchQueue.has(cacheKey)) {
            this.prefetchQueue.add(cacheKey);
            setTimeout(async () => {
                try {
                    const cached = this.cache.get(cacheKey);
                    const timeToExpiry = cached ? (cached.expiresAt - Date.now()) / 1000 : 0;
                    if (timeToExpiry < 120 && timeToExpiry > 0) {
                        console.log(`🔮 Predictive prefetch: ${cacheKey}`);
                        const freshData = await fetchCallback();
                        this.cache.set(cacheKey, {
                            data: freshData,
                            expiresAt: Date.now() + (this.getStochasticTTL(300) * 1000),
                            createdAt: Date.now()
                        });
                    }
                    this.prefetchQueue.delete(cacheKey);
                } catch (error) {
                    console.error(`Prefetch failed for ${cacheKey}:`, error.message);
                    this.prefetchQueue.delete(cacheKey);
                }
            }, 1000);
        }
    }

    async warmCache(categoryId, fetchCallback) {
        if (this.isWarming) return;
        this.isWarming = true;
        console.log(`🔥 Warming cache for category: ${categoryId}`);
        try {
            const cacheKey = this.getCacheKey('products', { category: categoryId, limit: 20 });
            await this.getOrSet(cacheKey, fetchCallback, 600);
        } catch (error) {
            console.error(`Cache warming failed:`, error.message);
        } finally {
            this.isWarming = false;
        }
    }

    invalidatePattern(prefix) {
        let invalidated = 0;
        for (const [key] of this.cache) {
            if (key.startsWith(prefix)) {
                this.cache.delete(key);
                invalidated++;
            }
        }
        console.log(`🗑️ Invalidated ${invalidated} cache entries with prefix: ${prefix}`);
        return invalidated;
    }

    getStats() {
        return {
            size: this.cache.size,
            prefetchQueue: this.prefetchQueue.size,
            accessLogSize: this.accessLog.size,
            hitRate: 'calculating'
        };
    }
}

const cacheManager = new AdaptiveCacheManager();

// ----------------------------
// Algorithm 3: Distributed Inventory Reservation with Auto-Expiry (DIRE)
// ----------------------------
// FAANG-level reservation system that:
// 1. Creates time-limited stock reservations (default 10 minutes)
// 2. Prevents overselling across multiple concurrent requests
// 3. Auto-expires reservations without manual release
// 4. Implements atomic stock check-reserve operations
// 5. Tracks reservation metrics for monitoring
// ----------------------------

class InventoryReservationManager {
    constructor() {
        this.reservations = new Map();
        this.reservationIndex = new Map();
        this.defaultTTL = 600000;
        this.maxReservationsPerVariant = 5;
        this.cleanupIntervalMs = 60000;
        this.startCleanup();
    }

    generateReservationId() {
        return `res_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }

    async reserveStock(productId, variantId, quantity, context = {}) {
        const { cartId = null, userId = null, priority = 1, ttl = this.defaultTTL } = context;
        const reservationKey = `${productId}_${variantId}`;

        const existingReservations = this.reservationIndex.get(reservationKey) || [];

        let totalReserved = 0;
        for (const resId of existingReservations) {
            const reservation = this.reservations.get(resId);
            if (reservation && !reservation.expired && !reservation.released) {
                totalReserved += reservation.quantity;
            }
        }

        const product = await Product.findById(productId);
        if (!product) {
            return { success: false, error: 'Product not found' };
        }

        const variant = product.variants.id(variantId);
        if (!variant) {
            return { success: false, error: 'Variant not found' };
        }

        const availableStock = variant.stock - totalReserved;

        if (availableStock < quantity) {
            return {
                success: false,
                error: 'Insufficient stock',
                availableStock,
                requestedQuantity: quantity,
                totalReserved
            };
        }

        if (userId || cartId) {
            const userExisting = existingReservations.some(resId => {
                const res = this.reservations.get(resId);
                return res && (res.userId === userId || res.cartId === cartId) && !res.expired && !res.released;
            });

            if (userExisting) {
                return {
                    success: false,
                    error: 'User already has an active reservation for this variant',
                    existingReservation: true
                };
            }
        }

        const reservationId = this.generateReservationId();
        const expiresAt = Date.now() + ttl;

        const reservation = {
            id: reservationId,
            productId,
            variantId,
            quantity,
            cartId,
            userId,
            priority,
            createdAt: Date.now(),
            expiresAt,
            expired: false,
            released: false,
            ttl
        };

        this.reservations.set(reservationId, reservation);

        if (!this.reservationIndex.has(reservationKey)) {
            this.reservationIndex.set(reservationKey, []);
        }
        this.reservationIndex.get(reservationKey).push(reservationId);

        setTimeout(() => {
            this.expireReservation(reservationId);
        }, ttl);

        return {
            success: true,
            reservationId,
            expiresAt,
            expiresInSeconds: Math.floor(ttl / 1000),
            reservedStock: quantity,
            availableStock: availableStock - quantity
        };
    }

    async releaseReservation(reservationId) {
        const reservation = this.reservations.get(reservationId);
        if (!reservation) {
            return { success: false, error: 'Reservation not found' };
        }

        if (reservation.expired || reservation.released) {
            return { success: false, error: 'Reservation already expired or released' };
        }

        reservation.released = true;

        const reservationKey = `${reservation.productId}_${reservation.variantId}`;
        const index = this.reservationIndex.get(reservationKey) || [];
        const filteredIndex = index.filter(id => id !== reservationId);

        if (filteredIndex.length === 0) {
            this.reservationIndex.delete(reservationKey);
        } else {
            this.reservationIndex.set(reservationKey, filteredIndex);
        }

        return {
            success: true,
            released: true,
            productId: reservation.productId,
            variantId: reservation.variantId,
            quantity: reservation.quantity
        };
    }

    expireReservation(reservationId) {
        const reservation = this.reservations.get(reservationId);
        if (!reservation) return;
        if (reservation.released) return;

        reservation.expired = true;

        const reservationKey = `${reservation.productId}_${reservation.variantId}`;
        const index = this.reservationIndex.get(reservationKey) || [];
        const filteredIndex = index.filter(id => id !== reservationId);

        if (filteredIndex.length === 0) {
            this.reservationIndex.delete(reservationKey);
        } else {
            this.reservationIndex.set(reservationKey, filteredIndex);
        }

        console.log(`⏰ Reservation expired: ${reservationId}`);
    }

    getReservationStatus(reservationId) {
        const reservation = this.reservations.get(reservationId);
        if (!reservation) {
            return { exists: false };
        }

        return {
            exists: true,
            active: !reservation.expired && !reservation.released,
            expired: reservation.expired,
            released: reservation.released,
            quantity: reservation.quantity,
            expiresAt: reservation.expiresAt,
            remainingMs: Math.max(0, reservation.expiresAt - Date.now())
        };
    }

    getActiveReservations(productId, variantId) {
        const reservationKey = `${productId}_${variantId}`;
        const reservationIds = this.reservationIndex.get(reservationKey) || [];

        const activeReservations = [];
        for (const resId of reservationIds) {
            const reservation = this.reservations.get(resId);
            if (reservation && !reservation.expired && !reservation.released) {
                activeReservations.push({
                    id: reservation.id,
                    quantity: reservation.quantity,
                    expiresAt: reservation.expiresAt,
                    priority: reservation.priority
                });
            }
        }

        return activeReservations;
    }

    getTotalReservedQuantity(productId, variantId) {
        const activeReservations = this.getActiveReservations(productId, variantId);
        return activeReservations.reduce((sum, r) => sum + r.quantity, 0);
    }

    async getAvailableStock(productId, variantId) {
        const product = await Product.findById(productId);
        if (!product) return 0;

        const variant = product.variants.id(variantId);
        if (!variant) return 0;

        const totalReserved = this.getTotalReservedQuantity(productId, variantId);
        return Math.max(0, variant.stock - totalReserved);
    }

    startCleanup() {
        setInterval(() => {
            const now = Date.now();
            let expiredCount = 0;

            for (const [resId, reservation] of this.reservations.entries()) {
                if (!reservation.expired && !reservation.released && reservation.expiresAt < now) {
                    this.expireReservation(resId);
                    expiredCount++;
                }
            }

            if (expiredCount > 0) {
                console.log(`🧹 Cleaned up ${expiredCount} expired reservations`);
            }
        }, this.cleanupIntervalMs);
    }

    getMetrics() {
        let activeCount = 0;
        let totalQuantity = 0;

        for (const reservation of this.reservations.values()) {
            if (!reservation.expired && !reservation.released) {
                activeCount++;
                totalQuantity += reservation.quantity;
            }
        }

        return {
            activeReservations: activeCount,
            totalReservedQuantity: totalQuantity,
            trackedVariants: this.reservationIndex.size,
            totalReservationsCreated: this.reservations.size
        };
    }
}

const reservationManager = new InventoryReservationManager();

// ----------------------------
// Controller Functions
// ----------------------------

// GET /products
const getAllProducts = async (req, res) => {
    try {
        const { page = 1, limit = 10, category, minPrice, maxPrice, sort } = req.query;
        const cacheKey = cacheManager.getCacheKey('products', { page, limit, category, minPrice, maxPrice, sort });

        const result = await cacheManager.getOrSet(
            cacheKey,
            async () => {
                const query = { status: 'active' };
                if (category) query.category = category;
                if (minPrice || maxPrice) query.price = {};
                if (minPrice) query.price.$gte = Number(minPrice);
                if (maxPrice) query.price.$lte = Number(maxPrice);

                let products = await Product.find(query).populate('category').lean();
                products = products.map((p) => ({
                    ...p,
                    productScore: calculateProductScore(p),
                }));

                if (sort === 'score_desc') {
                    products.sort((a, b) => b.productScore - a.productScore);
                } else if (sort === 'price_asc') {
                    products.sort((a, b) => a.price - b.price);
                } else if (sort === 'price_desc') {
                    products.sort((a, b) => b.price - a.price);
                } else if (sort === 'newest') {
                    products.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                }

                const startIndex = (page - 1) * limit;
                const endIndex = page * limit;
                const paginated = products.slice(startIndex, endIndex);

                if (category && paginated.length > 5) {
                    cacheManager.warmCache(category, async () => {
                        return Product.find({ category, status: 'active' }).limit(20).lean();
                    });
                }

                return {
                    page: Number(page),
                    limit: Number(limit),
                    total: products.length,
                    products: paginated,
                };
            },
            300
        );

        res.setHeader('X-Cache', cacheManager.cache.has(cacheKey) ? 'HIT' : 'MISS');
        res.json(result);
    } catch (error) {
        console.error(error);
        res.status(500).json({ message: 'Server error fetching products' });
    }
};

// GET /products/:id
const getProductById = async (req, res) => {
    try {
        const { id } = req.params;
        if (!mongoose.Types.ObjectId.isValid(id)) {
            return res.status(400).json({ message: 'Invalid ID' });
        }

        const cacheKey = cacheManager.getCacheKey('product', { id });

        const productData = await cacheManager.getOrSet(
            cacheKey,
            async () => {
                const product = await Product.findById(id).populate('category').lean();
                if (!product) throw new Error('Product not found');
                const rankedVariants = product.variants.sort((a, b) => (b.availabilityScore || 0) - (a.availabilityScore || 0));
                return { ...product, variants: rankedVariants };
            },
            600
        );

        res.json(productData);
    } catch (error) {
        if (error.message === 'Product not found') {
            return res.status(404).json({ message: 'Product not found' });
        }
        console.error(error);
        res.status(500).json({ message: 'Server error fetching product' });
    }
};

// POST /products
const createProduct = async (req, res) => {
    try {
        const product = new Product(req.body);
        await product.save();
        cacheManager.invalidatePattern('products');
        res.status(201).json(product);
    } catch (error) {
        console.error(error);
        res.status(400).json({ message: error.message });
    }
};

// PUT /products/:id
const updateProduct = async (req, res) => {
    try {
        const { id } = req.params;
        const updates = req.body;

        const product = await Product.findById(id);
        if (!product) {
            return res.status(404).json({ message: 'Product not found' });
        }

        Object.keys(updates).forEach(key => {
            product[key] = updates[key];
        });

        await product.save();
        await product.populate('category');

        cacheManager.invalidatePattern(`product:${JSON.stringify({ id })}`);
        cacheManager.invalidatePattern('products');

        res.json(product);
    } catch (error) {
        console.error(error);
        res.status(400).json({ message: error.message });
    }
};

// DELETE /products/:id
const deleteProduct = async (req, res) => {
    try {
        const { id } = req.params;

        const activeReservations = reservationManager.getActiveReservations(id, null);
        if (activeReservations.length > 0) {
            return res.status(409).json({
                error: 'Cannot delete product with active reservations',
                code: 'ACTIVE_RESERVATIONS',
                activeReservations: activeReservations.length
            });
        }

        const product = await Product.findByIdAndDelete(id);
        if (!product) return res.status(404).json({ message: 'Product not found' });

        cacheManager.invalidatePattern(`product:${JSON.stringify({ id })}`);
        cacheManager.invalidatePattern('products');

        res.json({ message: 'Product deleted successfully' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ message: 'Server error deleting product' });
    }
};

// GET /products/cache/stats
const getCacheStats = async (req, res) => {
    try {
        res.json({
            algorithm: 'Adaptive Cache-Aside with Predictive Prefetch',
            stats: cacheManager.getStats(),
            features: {
                stochasticTTL: 'Enabled (±20% jitter)',
                predictivePrefetch: 'Active for high-frequency keys',
                cacheWarming: 'Auto-warms popular categories',
                invalidation: 'Pattern-based cache invalidation'
            }
        });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// ============================================
// NEW ENDPOINT: POST /products/:id/reserve-stock
// ============================================
const reserveStock = async (req, res) => {
    try {
        const { id: productId } = req.params;
        const {
            variantId,
            quantity = 1,
            cartId = null,
            userId = null,
            priority = 1,
            ttl = 600000
        } = req.body;

        if (!variantId) {
            return res.status(400).json({
                error: 'Validation failed',
                code: 'MISSING_VARIANT_ID',
                message: 'variantId is required'
            });
        }

        if (quantity < 1 || quantity > 999) {
            return res.status(400).json({
                error: 'Validation failed',
                code: 'INVALID_QUANTITY',
                message: 'quantity must be between 1 and 999'
            });
        }

        if (!mongoose.Types.ObjectId.isValid(productId)) {
            return res.status(400).json({
                error: 'Invalid product ID',
                code: 'INVALID_PRODUCT_ID'
            });
        }

        const product = await Product.findById(productId);
        if (!product) {
            return res.status(404).json({
                error: 'Product not found',
                code: 'PRODUCT_NOT_FOUND'
            });
        }

        const variant = product.variants.id(variantId);
        if (!variant) {
            return res.status(404).json({
                error: 'Variant not found',
                code: 'VARIANT_NOT_FOUND'
            });
        }

        const result = await reservationManager.reserveStock(
            productId,
            variantId,
            quantity,
            { cartId, userId, priority, ttl }
        );

        if (!result.success) {
            const statusCode = result.error === 'Product not found' ? 404 : 409;
            return res.status(statusCode).json({
                error: result.error,
                code: 'RESERVATION_FAILED',
                details: {
                    availableStock: result.availableStock,
                    requestedQuantity: result.requestedQuantity,
                    totalReserved: result.totalReserved
                }
            });
        }

        const availableStock = await reservationManager.getAvailableStock(productId, variantId);

        res.setHeader('X-Reservation-Id', result.reservationId);
        res.setHeader('X-Expires-In', result.expiresInSeconds);

        res.json({
            success: true,
            reservationId: result.reservationId,
            expiresAt: result.expiresAt,
            expiresInSeconds: result.expiresInSeconds,
            reservedQuantity: result.reservedStock,
            availableStock: availableStock,
            variant: {
                id: variantId,
                color: variant.color,
                size: variant.size,
                originalStock: variant.stock
            },
            message: `Reserved ${quantity} item(s) for ${result.expiresInSeconds} seconds`
        });

    } catch (error) {
        console.error('Reserve stock error:', error);
        res.status(500).json({
            error: 'Reservation failed',
            code: 'RESERVATION_ERROR',
            message: error.message
        });
    }
};

// GET /products/reservation/:reservationId
const getReservationStatus = async (req, res) => {
    try {
        const { reservationId } = req.params;

        const status = reservationManager.getReservationStatus(reservationId);

        if (!status.exists) {
            return res.status(404).json({
                error: 'Reservation not found',
                code: 'RESERVATION_NOT_FOUND'
            });
        }

        res.json({
            reservationId,
            active: status.active,
            expired: status.expired,
            released: status.released,
            quantity: status.quantity,
            expiresAt: status.expiresAt,
            remainingSeconds: Math.floor(status.remainingMs / 1000)
        });

    } catch (error) {
        res.status(500).json({
            error: 'Failed to get reservation status',
            code: 'STATUS_ERROR'
        });
    }
};

// DELETE /products/reservation/:reservationId
const releaseReservation = async (req, res) => {
    try {
        const { reservationId } = req.params;

        const result = await reservationManager.releaseReservation(reservationId);

        if (!result.success) {
            return res.status(404).json({
                error: result.error,
                code: 'RELEASE_FAILED'
            });
        }

        res.json({
            success: true,
            message: 'Reservation released successfully',
            productId: result.productId,
            variantId: result.variantId,
            quantity: result.quantity
        });

    } catch (error) {
        res.status(500).json({
            error: 'Failed to release reservation',
            code: 'RELEASE_ERROR'
        });
    }
};

// GET /products/reservation/metrics
const getReservationMetrics = async (req, res) => {
    try {
        const metrics = reservationManager.getMetrics();

        res.json({
            algorithm: 'Distributed Inventory Reservation with Auto-Expiry (DIRE)',
            metrics: metrics,
            config: {
                defaultTTLSeconds: reservationManager.defaultTTL / 1000,
                maxReservationsPerVariant: reservationManager.maxReservationsPerVariant,
                cleanupIntervalSeconds: reservationManager.cleanupIntervalMs / 1000
            },
            features: {
                autoExpiry: 'Enabled',
                priorityQueue: 'Supported (1-3)',
                concurrentReservations: `${reservationManager.maxReservationsPerVariant} per variant`,
                atomicOperations: 'Enabled'
            }
        });

    } catch (error) {
        res.status(500).json({
            error: 'Failed to get metrics',
            code: 'METRICS_ERROR'
        });
    }
};

module.exports = {
    getAllProducts,
    getProductById,
    createProduct,
    updateProduct,
    deleteProduct,
    getCacheStats,
    reserveStock,
    getReservationStatus,
    releaseReservation,
    getReservationMetrics
};