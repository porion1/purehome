const mongoose = require('mongoose');

// ----------------------------
// Category Schema - FAANG-Level MVP
// ----------------------------
const categorySchema = new mongoose.Schema(
    {
        name: {
            type: String,
            required: [true, 'Category name is required'],
            trim: true,
            unique: true,
            index: true, // Fast search
        },
        description: {
            type: String,
            trim: true,
        },
        status: {
            type: String,
            enum: ['active', 'inactive'],
            default: 'active',
            index: true,
        },
        rankScore: {
            type: Number,
            default: 0, // 🚀 Algorithm 1 Field
            index: true,
        },
        productCount: {
            type: Number,
            default: 0,
            index: true,
        },

        // ============================================
        // ALGORITHM 2: Adaptive Category Mesh Fields
        // ============================================
        popularityPulse: {
            type: {
                current: { type: Number, default: 0, min: 0, max: 100 },      // 0-100 real-time popularity
                trend: { type: String, enum: ['RISING', 'STABLE', 'FALLING'], default: 'STABLE' },
                peakHour: { type: String, default: '' },                        // Hour of highest activity
                lastCalculated: { type: Date, default: Date.now },
            },
            default: {},
        },

        clickVelocity: {
            type: Number,
            default: 0,        // Clicks per hour (rolling average)
            min: 0,
        },

        conversionRate: {
            type: Number,
            default: 0,        // Percentage of category views → product purchases
            min: 0,
            max: 100,
        },

        relatedCategories: {
            type: [{
                categoryId: { type: mongoose.Schema.Types.ObjectId, ref: 'Category' },
                strengthScore: { type: Number, default: 0, min: 0, max: 100 }, // How strongly related
            }],
            default: [],
            index: true,
        },

        seasonalBoost: {
            type: Number,
            default: 1.0,      // Multiplier for seasonal factors (0.5 = winter, 1.5 = peak season)
            min: 0.5,
            max: 2.0,
        },
    },
    { timestamps: true }
);

// ============================================
// ALGORITHM 1: Dynamic Category Ranking (Existing)
// ============================================
// Calculates category rankScore automatically based on:
// 1. Number of active products
// 2. Optional trend score (future analytics input)
// rankScore = productCount * trendScore (default 5)
// ============================================
categorySchema.pre('save', function (next) {
    const trendScore = this.trendScore || 5;
    this.rankScore = this.productCount * trendScore;
    next();
});

// ============================================
// ALGORITHM 2: Adaptive Category Mesh + Real-time Popularity Pulse (NEW)
// ============================================
// FAANG-level algorithm that:
// 1. Calculates real-time popularity pulse (0-100) based on click velocity + conversion
// 2. Auto-detects trend direction (RISING/STABLE/FALLING) using 3-point moving average
// 3. Builds related category mesh based on user co-click patterns
// 4. Applies seasonal boost without manual intervention
// 5. Enables predictive category ordering for homepage & search
// ============================================

// Helper: Calculate popularity pulse (0-100 scale)
function calculatePopularityPulse(clickVelocity, conversionRate, productCount) {
    // Normalize click velocity (assume max 1000 clicks/hour = 100%)
    const normalizedVelocity = Math.min(100, (clickVelocity / 1000) * 100);

    // Weighted formula: 40% velocity + 40% conversion + 20% product count
    let pulse = (normalizedVelocity * 0.4) + (conversionRate * 0.4) + (Math.min(100, productCount * 2) * 0.2);

    return Math.floor(Math.min(100, Math.max(0, pulse)));
}

// Helper: Determine trend using 3-point moving average
function determineTrend(historicalPulses) {
    if (!historicalPulses || historicalPulses.length < 3) return 'STABLE';

    const last3 = historicalPulses.slice(-3);
    const avg1 = last3[0];
    const avg2 = last3[1];
    const avg3 = last3[2];

    if (avg3 > avg2 && avg2 > avg1) return 'RISING';
    if (avg3 < avg2 && avg2 < avg1) return 'FALLING';
    return 'STABLE';
}

// Helper: Find peak activity hour (simulated - would come from analytics)
function detectPeakHour() {
    const hour = new Date().getHours();
    const peakHours = ['10:00', '14:00', '19:00', '21:00'];
    return peakHours[Math.floor(Math.random() * peakHours.length)];
}

// Main algorithm: Update popularity metrics
categorySchema.methods.updatePopularityPulse = async function(clickData = null) {
    // In production, clickData would come from real analytics middleware
    // For MVP, simulate or use provided data

    if (clickData) {
        this.clickVelocity = clickData.clicksPerHour || this.clickVelocity;
        this.conversionRate = clickData.conversionRate || this.conversionRate;
    } else {
        // Simulate realistic decay if no data (prevents stale metrics)
        this.clickVelocity = Math.max(0, this.clickVelocity * 0.95);
        this.conversionRate = Math.max(0, this.conversionRate * 0.98);
    }

    // Store historical pulses for trend detection (in real impl, use separate collection)
    const historicalPulses = this._historicalPulses || [];
    const currentPulse = calculatePopularityPulse(
        this.clickVelocity,
        this.conversionRate,
        this.productCount
    );

    historicalPulses.push(currentPulse);
    if (historicalPulses.length > 10) historicalPulses.shift();
    this._historicalPulses = historicalPulses;

    // Update popularity pulse object
    this.popularityPulse = {
        current: currentPulse,
        trend: determineTrend(historicalPulses),
        peakHour: detectPeakHour(),
        lastCalculated: new Date()
    };

    // Apply seasonal boost to rankScore (Algorithm 1 enhancement)
    const baseRankScore = this.productCount * (this.trendScore || 5);
    this.rankScore = Math.floor(baseRankScore * this.seasonalBoost);

    return this;
};

// Build related category mesh based on co-occurrence
categorySchema.methods.buildRelatedMesh = async function(otherCategoryIds, strengthScores) {
    // otherCategoryIds: array of category IDs that users frequently browse together
    // strengthScores: corresponding strength scores (0-100)

    if (!otherCategoryIds || otherCategoryIds.length === 0) return this;

    this.relatedCategories = otherCategoryIds.map((id, idx) => ({
        categoryId: id,
        strengthScore: strengthScores[idx] || 50
    }));

    // Sort by strength score descending
    this.relatedCategories.sort((a, b) => b.strengthScore - a.strengthScore);

    // Keep top 10 related categories only (performance)
    if (this.relatedCategories.length > 10) {
        this.relatedCategories = this.relatedCategories.slice(0, 10);
    }

    return this;
};

// Auto-update before save if metrics changed
categorySchema.pre('save', async function(next) {
    const metricsChanged = this.isModified('clickVelocity') ||
        this.isModified('conversionRate') ||
        this.isModified('productCount');

    if (metricsChanged || this.isNew) {
        try {
            await this.updatePopularityPulse();
        } catch (error) {
            console.error(`[Category: ${this.name}] Popularity pulse update failed:`, error.message);
        }
    }
    next();
});

// ============================================
// STATIC METHODS FOR FAANG-SCALE QUERIES
// ============================================

// Get trending categories (rising + high popularity)
categorySchema.statics.getTrendingCategories = function(limit = 10) {
    return this.find({
        status: 'active',
        'popularityPulse.trend': 'RISING',
        'popularityPulse.current': { $gte: 30 }
    })
        .sort({ 'popularityPulse.current': -1, rankScore: -1 })
        .limit(limit)
        .select('name popularityPulse rankScore productCount');
};

// Get category recommendations based on related mesh
categorySchema.statics.getRecommendations = async function(categoryId, limit = 5) {
    const category = await this.findById(categoryId).select('relatedCategories');
    if (!category || !category.relatedCategories.length) return [];

    const relatedIds = category.relatedCategories.map(r => r.categoryId);
    return this.find({
        _id: { $in: relatedIds },
        status: 'active'
    })
        .sort({ 'popularityPulse.current': -1 })
        .limit(limit);
};

// Bulk update seasonal boost for multiple categories (e.g., holiday season)
categorySchema.statics.applySeasonalBoost = async function(categoryIds, boostMultiplier) {
    return this.updateMany(
        { _id: { $in: categoryIds } },
        { $mul: { seasonalBoost: boostMultiplier } }
    );
};

// Get category performance dashboard data
categorySchema.statics.getPerformanceDashboard = async function() {
    const aggregation = await this.aggregate([
        { $match: { status: 'active' } },
        {
            $group: {
                _id: null,
                avgPopularity: { $avg: '$popularityPulse.current' },
                avgConversion: { $avg: '$conversionRate' },
                totalProducts: { $sum: '$productCount' },
                risingCount: {
                    $sum: { $cond: [{ $eq: ['$popularityPulse.trend', 'RISING'] }, 1, 0] }
                },
                criticalCount: {
                    $sum: { $cond: [{ $lt: ['$popularityPulse.current', 20] }, 1, 0] }
                }
            }
        }
    ]);

    return aggregation[0] || {
        avgPopularity: 0,
        avgConversion: 0,
        totalProducts: 0,
        risingCount: 0,
        criticalCount: 0
    };
};

// ============================================
// INDEXING FOR FAANG-SCALE QUERIES
// ============================================
categorySchema.index({ name: 'text', description: 'text' });                    // search
categorySchema.index({ rankScore: -1 });                                        // top categories
categorySchema.index({ 'popularityPulse.current': -1, status: 1 });             // trending
categorySchema.index({ 'popularityPulse.trend': 1, status: 1 });                // trend filter
categorySchema.index({ clickVelocity: -1 });                                    // high velocity
categorySchema.index({ relatedCategories: 1 });                                 // mesh queries

// ============================================
// VIRTUAL PROPERTIES (enhanced UX)
// ============================================
categorySchema.virtual('isTrending').get(function() {
    return this.popularityPulse?.trend === 'RISING' && this.popularityPulse?.current >= 50;
});

categorySchema.virtual('healthStatus').get(function() {
    if (this.popularityPulse?.current >= 70) return 'EXCELLENT';
    if (this.popularityPulse?.current >= 40) return 'GOOD';
    if (this.popularityPulse?.current >= 20) return 'WARNING';
    return 'CRITICAL';
});

// Ensure virtuals are included in JSON output
categorySchema.set('toJSON', { virtuals: true });
categorySchema.set('toObject', { virtuals: true });

// ----------------------------
// Export Model
// ----------------------------
module.exports = mongoose.model('Category', categorySchema);