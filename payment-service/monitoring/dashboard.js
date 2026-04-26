/**
 * ============================================================
 * 📊 DASHBOARD.JS — GRAFANA DASHBOARD CONFIGURATION v1.0
 * ============================================================
 *
 * PURPOSE:
 * - Export Grafana dashboard configuration as JSON
 * - Pre-configured dashboards for payment service monitoring
 * - Real-time metrics visualization at 50M scale
 * - Alert rules integration
 *
 * SCALE TARGET:
 * - 50M+ users
 * - Real-time dashboard updates (1s refresh)
 * - 30-day data retention
 *
 * ============================================================
 *
 * 🧠 ALGORITHM 1: QUERY OPTIMIZATION (PromQL Query Generator)
 * ------------------------------------------------------------
 * - Generates optimized PromQL queries for dashboard panels
 * - Rate-limit aware queries to prevent overloading Prometheus
 * - Automatic downsampling for long time ranges
 *
 * 🧠 ALGORITHM 2: DASHBOARD AUTO-REFRESH (Adaptive Refresh Rate)
 * ------------------------------------------------------------
 * - Dynamically adjusts refresh rate based on time range
 * - 5s for last hour, 30s for last day, 5m for last week
 * - Prevents browser overload
 *
 * ============================================================
 */

// ============================================================
// 🧠 ALGORITHM 1: QUERY OPTIMIZATION
// ============================================================

class PromQLOptimizer {
    constructor() {
        this.queryCache = new Map();
        this.cacheTTL = 60000; // 1 minute
        this.stats = {
            optimizedQueries: 0,
            cacheHits: 0,
            avgQueryComplexity: 0,
        };
    }

    /**
     * Optimize PromQL query for performance
     */
    optimizeQuery(metricName, filters, timeRange, options = {}) {
        const cacheKey = `${metricName}:${JSON.stringify(filters)}:${timeRange}`;

        // Check cache
        if (this.queryCache.has(cacheKey)) {
            const cached = this.queryCache.get(cacheKey);
            if (Date.now() - cached.timestamp < this.cacheTTL) {
                this.stats.cacheHits++;
                return cached.query;
            }
        }

        let query = metricName;

        // Add filters
        if (filters && Object.keys(filters).length > 0) {
            const filterStr = Object.entries(filters)
                .map(([k, v]) => `${k}="${v}"`)
                .join(',');
            query += `{${filterStr}}`;
        }

        // Apply rate function for counter metrics (prevents overflow)
        if (options.isCounter && options.rateWindow) {
            query = `rate(${query}[${options.rateWindow}])`;
        }

        // Apply downsampling for long time ranges
        if (timeRange > 86400) { // > 24 hours
            const step = this.calculateOptimalStep(timeRange);
            query = `avg_over_time(${query}[${step}])`;
        }

        // Apply aggregation for high-cardinality metrics
        if (options.aggregateBy) {
            query = `sum by (${options.aggregateBy}) (${query})`;
        }

        this.stats.optimizedQueries++;
        this.stats.avgQueryComplexity =
            (this.stats.avgQueryComplexity * (this.stats.optimizedQueries - 1) + query.length) /
            this.stats.optimizedQueries;

        // Cache the result
        this.queryCache.set(cacheKey, {
            query,
            timestamp: Date.now(),
        });

        return query;
    }

    /**
     * Calculate optimal step size for time range
     */
    calculateOptimalStep(timeRangeSeconds) {
        if (timeRangeSeconds <= 3600) return '5s';      // 1 hour → 5s
        if (timeRangeSeconds <= 86400) return '30s';    // 24 hours → 30s
        if (timeRangeSeconds <= 604800) return '5m';    // 7 days → 5m
        if (timeRangeSeconds <= 2592000) return '30m';  // 30 days → 30m
        return '1h';                                     // >30 days → 1h
    }

    getMetrics() {
        return {
            optimizedQueries: this.stats.optimizedQueries,
            cacheHits: this.stats.cacheHits,
            cacheHitRate: this.stats.optimizedQueries > 0
                ? ((this.stats.cacheHits / this.stats.optimizedQueries) * 100).toFixed(2) + '%'
                : '0%',
            avgQueryComplexity: Math.round(this.stats.avgQueryComplexity),
        };
    }
}

// ============================================================
// 🧠 ALGORITHM 2: ADAPTIVE REFRESH RATE
// ============================================================

class AdaptiveRefreshCalculator {
    constructor() {
        this.refreshRates = {
            '1h': '5s',
            '6h': '10s',
            '12h': '30s',
            '24h': '1m',
            '7d': '5m',
            '30d': '15m',
            'default': '30s',
        };
        this.stats = {
            totalCalculations: 0,
            refreshChanges: 0,
        };
    }

    /**
     * Calculate optimal refresh rate based on time range
     */
    calculateRefreshRate(timeRange) {
        this.stats.totalCalculations++;

        let refreshRate = this.refreshRates.default;

        if (timeRange <= 3600) { // 1 hour
            refreshRate = this.refreshRates['1h'];
        } else if (timeRange <= 21600) { // 6 hours
            refreshRate = this.refreshRates['6h'];
        } else if (timeRange <= 43200) { // 12 hours
            refreshRate = this.refreshRates['12h'];
        } else if (timeRange <= 86400) { // 24 hours
            refreshRate = this.refreshRates['24h'];
        } else if (timeRange <= 604800) { // 7 days
            refreshRate = this.refreshRates['7d'];
        } else if (timeRange <= 2592000) { // 30 days
            refreshRate = this.refreshRates['30d'];
        }

        return refreshRate;
    }

    /**
     * Get dashboard configuration with adaptive refresh
     */
    getDashboardConfig(timeRange) {
        const refreshRate = this.calculateRefreshRate(timeRange);

        return {
            refresh: refreshRate,
            time: {
                from: `now-${this.formatTimeRange(timeRange)}`,
                to: 'now',
            },
        };
    }

    formatTimeRange(seconds) {
        if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
        if (seconds < 86400) return `${Math.floor(seconds / 3600)}h`;
        if (seconds < 604800) return `${Math.floor(seconds / 86400)}d`;
        return `${Math.floor(seconds / 86400)}d`;
    }

    getMetrics() {
        return {
            totalCalculations: this.stats.totalCalculations,
            refreshChanges: this.stats.refreshChanges,
        };
    }
}

// ============================================================
// 🔧 INITIALIZE ALGORITHMS
// ============================================================

const promQLOptimizer = new PromQLOptimizer();
const adaptiveRefresh = new AdaptiveRefreshCalculator();

// ============================================================
// 📊 DASHBOARD PANEL GENERATORS
// ============================================================

class DashboardPanelGenerator {
    /**
     * Generate payment volume panel
     */
    static paymentVolumePanel() {
        const query = promQLOptimizer.optimizeQuery(
            'payment_transactions_total',
            { status: 'succeeded' },
            86400,
            { isCounter: true, rateWindow: '1m', aggregateBy: 'payment_method' }
        );

        return {
            title: 'Payment Volume (Last 24h)',
            type: 'graph',
            gridPos: { x: 0, y: 0, w: 12, h: 8 },
            targets: [{ expr: query, legendFormat: '{{payment_method}}' }],
            fieldConfig: {
                defaults: {
                    unit: 'cpm',
                    decimals: 0,
                    custom: { lineWidth: 2, fillOpacity: 10 },
                },
            },
            options: {
                legend: { displayMode: 'list', placement: 'bottom' },
                tooltip: { mode: 'multi' },
            },
        };
    }

    /**
     * Generate payment success rate panel
     */
    static successRatePanel() {
        const successQuery = promQLOptimizer.optimizeQuery(
            'payment_transactions_total',
            { status: 'succeeded' },
            86400,
            { isCounter: true, rateWindow: '1m' }
        );

        const totalQuery = promQLOptimizer.optimizeQuery(
            'payment_transactions_total',
            {},
            86400,
            { isCounter: true, rateWindow: '1m' }
        );

        return {
            title: 'Payment Success Rate',
            type: 'stat',
            gridPos: { x: 12, y: 0, w: 6, h: 4 },
            targets: [
                { expr: `(${successQuery} / ${totalQuery}) * 100`, legendFormat: 'Success Rate' },
            ],
            fieldConfig: {
                defaults: {
                    unit: 'percent',
                    decimals: 1,
                    thresholds: {
                        steps: [
                            { color: 'red', value: null },
                            { color: 'yellow', value: 95 },
                            { color: 'green', value: 99 },
                        ],
                    },
                },
            },
            options: {
                reduceOptions: { calcs: ['lastNotNull'] },
                textMode: 'value_and_name',
            },
        };
    }

    /**
     * Generate response time panel (p50, p95, p99)
     */
    static responseTimePanel() {
        const p50Query = promQLOptimizer.optimizeQuery(
            'payment_request_duration_seconds',
            { quantile: '0.5' },
            86400
        );
        const p95Query = promQLOptimizer.optimizeQuery(
            'payment_request_duration_seconds',
            { quantile: '0.95' },
            86400
        );
        const p99Query = promQLOptimizer.optimizeQuery(
            'payment_request_duration_seconds',
            { quantile: '0.99' },
            86400
        );

        return {
            title: 'API Response Time',
            type: 'graph',
            gridPos: { x: 0, y: 8, w: 12, h: 8 },
            targets: [
                { expr: p50Query, legendFormat: 'p50' },
                { expr: p95Query, legendFormat: 'p95' },
                { expr: p99Query, legendFormat: 'p99' },
            ],
            fieldConfig: {
                defaults: {
                    unit: 'ms',
                    decimals: 0,
                    custom: { lineWidth: 2, fillOpacity: 0 },
                },
            },
        };
    }

    /**
     * Generate error rate panel
     */
    static errorRatePanel() {
        const errorQuery = promQLOptimizer.optimizeQuery(
            'payment_errors_total',
            {},
            86400,
            { isCounter: true, rateWindow: '1m', aggregateBy: 'error_type' }
        );

        return {
            title: 'Error Rate by Type',
            type: 'piechart',
            gridPos: { x: 12, y: 4, w: 6, h: 6 },
            targets: [{ expr: errorQuery, legendFormat: '{{error_type}}' }],
            options: {
                pieType: 'pie',
                displayLabels: ['name', 'percent'],
                tooltip: { mode: 'single' },
            },
        };
    }

    /**
     * Generate circuit breaker status panel
     */
    static circuitBreakerPanel() {
        const query = promQLOptimizer.optimizeQuery(
            'circuit_breaker_state',
            { service: 'stripe' },
            3600
        );

        return {
            title: 'Circuit Breaker Status',
            type: 'stat',
            gridPos: { x: 18, y: 0, w: 6, h: 4 },
            targets: [{ expr: query, legendFormat: 'State' }],
            fieldConfig: {
                defaults: {
                    mappings: [
                        { type: 'value', value: '0', text: 'CLOSED', color: 'green' },
                        { type: 'value', value: '1', text: 'OPEN', color: 'red' },
                        { type: 'value', value: '2', text: 'HALF_OPEN', color: 'yellow' },
                    ],
                },
            },
        };
    }

    /**
     * Generate rate limiting panel
     */
    static rateLimitingPanel() {
        const throttledQuery = promQLOptimizer.optimizeQuery(
            'rate_limiter_throttled_total',
            {},
            86400,
            { isCounter: true, rateWindow: '1m' }
        );

        const remainingQuery = promQLOptimizer.optimizeQuery(
            'rate_limiter_tokens_remaining',
            {},
            86400
        );

        return {
            title: 'Rate Limiting',
            type: 'graph',
            gridPos: { x: 0, y: 16, w: 12, h: 8 },
            targets: [
                { expr: throttledQuery, legendFormat: 'Throttled Requests', yAxis: 2 },
                { expr: remainingQuery, legendFormat: 'Tokens Remaining', yAxis: 1 },
            ],
            fieldConfig: {
                defaults: {
                    custom: { lineWidth: 2, fillOpacity: 10 },
                },
            },
        };
    }

    /**
     * Generate idempotency cache panel
     */
    static idempotencyPanel() {
        const hitRateQuery = promQLOptimizer.optimizeQuery(
            'idempotency_cache_hit_rate',
            {},
            86400
        );

        return {
            title: 'Idempotency Cache Performance',
            type: 'gauge',
            gridPos: { x: 18, y: 4, w: 6, h: 6 },
            targets: [{ expr: hitRateQuery, legendFormat: 'Hit Rate' }],
            fieldConfig: {
                defaults: {
                    unit: 'percent',
                    decimals: 1,
                    min: 0,
                    max: 100,
                    thresholds: {
                        steps: [
                            { color: 'red', value: null },
                            { color: 'yellow', value: 70 },
                            { color: 'green', value: 90 },
                        ],
                    },
                },
            },
            options: {
                showThresholdLabels: true,
                showThresholdMarkers: true,
            },
        };
    }

    /**
     * Generate resource usage panel
     */
    static resourceUsagePanel() {
        const cpuQuery = promQLOptimizer.optimizeQuery(
            'process_cpu_seconds_total',
            {},
            86400,
            { isCounter: true, rateWindow: '1m' }
        );

        const memoryQuery = promQLOptimizer.optimizeQuery(
            'process_resident_memory_bytes',
            {},
            86400
        );

        return {
            title: 'Resource Usage',
            type: 'graph',
            gridPos: { x: 12, y: 10, w: 12, h: 8 },
            targets: [
                { expr: cpuQuery, legendFormat: 'CPU Usage', yAxis: 2 },
                { expr: memoryQuery, legendFormat: 'Memory (bytes)', yAxis: 1 },
            ],
            fieldConfig: {
                defaults: {
                    unit: 'bytes',
                    decimals: 0,
                    custom: { lineWidth: 2, fillOpacity: 10 },
                },
            },
        };
    }
}

// ============================================================
// 🚀 MAIN DASHBOARD CONFIGURATION
// ============================================================

/**
 * Generate complete Grafana dashboard configuration
 */
const generateDashboard = (options = {}) => {
    const {
        title = 'Payment Service Dashboard',
        timeRange = 86400, // 24 hours default
        tags = ['payment-service', 'production', '50m-scale'],
        uid = 'payment-service-dashboard',
    } = options;

    const dashboardConfig = adaptiveRefresh.getDashboardConfig(timeRange);

    return {
        dashboard: {
            id: null,
            uid: uid,
            title: title,
            tags: tags,
            timezone: 'browser',
            schemaVersion: 36,
            version: 1,
            refresh: dashboardConfig.refresh,
            time: dashboardConfig.time,
            timepicker: {
                refresh_intervals: ['5s', '10s', '30s', '1m', '5m', '15m', '30m', '1h'],
                time_options: ['5m', '15m', '1h', '6h', '12h', '24h', '2d', '7d', '30d'],
            },
            panels: [
                DashboardPanelGenerator.paymentVolumePanel(),
                DashboardPanelGenerator.successRatePanel(),
                DashboardPanelGenerator.circuitBreakerPanel(),
                DashboardPanelGenerator.responseTimePanel(),
                DashboardPanelGenerator.errorRatePanel(),
                DashboardPanelGenerator.idempotencyPanel(),
                DashboardPanelGenerator.rateLimitingPanel(),
                DashboardPanelGenerator.resourceUsagePanel(),
            ],
            templating: {
                list: [
                    {
                        name: 'service',
                        type: 'query',
                        query: 'label_values(payment_transactions_total, service)',
                        current: { value: 'payment-service' },
                        refresh: 1,
                        options: [],
                    },
                    {
                        name: 'environment',
                        type: 'query',
                        query: 'label_values(payment_transactions_total, environment)',
                        current: { value: 'production' },
                        refresh: 1,
                        options: [],
                    },
                ],
            },
            annotations: {
                list: [
                    {
                        name: 'Deployments',
                        type: 'grafana',
                        datasource: 'prometheus',
                        query: 'deployments_total',
                        iconColor: 'blue',
                        enable: true,
                    },
                    {
                        name: 'Circuit Breaker Events',
                        type: 'grafana',
                        datasource: 'prometheus',
                        query: 'circuit_breaker_events_total',
                        iconColor: 'red',
                        enable: true,
                    },
                ],
            },
            links: [
                {
                    title: 'Prometheus',
                    url: 'http://prometheus:9090',
                    type: 'dashboards',
                },
                {
                    title: 'Jaeger Tracing',
                    url: 'http://jaeger:16686',
                    type: 'dashboards',
                },
            ],
        },
        meta: {
            type: 'db',
            canSave: true,
            canEdit: true,
            canAdmin: true,
            version: 1,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        },
    };
};

/**
 * Generate lightweight dashboard (for development)
 */
const generateDevDashboard = () => {
    return generateDashboard({
        title: 'Payment Service Dashboard (Dev)',
        timeRange: 3600, // 1 hour
        tags: ['payment-service', 'development'],
        uid: 'payment-service-dashboard-dev',
    });
};

/**
 * Generate production dashboard (full metrics)
 */
const generateProdDashboard = () => {
    return generateDashboard({
        title: 'Payment Service Dashboard (Production)',
        timeRange: 86400, // 24 hours
        tags: ['payment-service', 'production', 'sla', '50m-scale'],
        uid: 'payment-service-dashboard-prod',
    });
};

// ============================================================
// 📊 DASHBOARD EXPORTERS
// ============================================================

const dashboardExporters = {
    /**
     * Export as JSON for Grafana API import
     */
    toJSON: (environment = 'production') => {
        const dashboard = environment === 'production'
            ? generateProdDashboard()
            : generateDevDashboard();
        return JSON.stringify(dashboard, null, 2);
    },

    /**
     * Export as curl command for Grafana API
     */
    toCurlCommand: (environment = 'production', grafanaUrl = 'http://localhost:3000', apiKey = '') => {
        const dashboard = environment === 'production'
            ? generateProdDashboard()
            : generateDevDashboard();

        return `curl -X POST ${grafanaUrl}/api/dashboards/db \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${apiKey}" \
  -d '${JSON.stringify(dashboard)}'`;
    },

    /**
     * Export as Kubernetes ConfigMap
     */
    toK8sConfigMap: (environment = 'production') => {
        const dashboard = environment === 'production'
            ? generateProdDashboard()
            : generateDevDashboard();

        return {
            apiVersion: 'v1',
            kind: 'ConfigMap',
            metadata: {
                name: `grafana-dashboard-${environment}`,
                namespace: 'monitoring',
                labels: {
                    'grafana_dashboard': '1',
                    'environment': environment,
                },
            },
            data: {
                [`payment-service-${environment}.json`]: JSON.stringify(dashboard, null, 2),
            },
        };
    },
};

// ============================================================
// 📊 METRICS
// ============================================================

const getDashboardMetrics = () => {
    return {
        promQLOptimizer: promQLOptimizer.getMetrics(),
        adaptiveRefresh: adaptiveRefresh.getMetrics(),
        panelCount: 8,
        templateVariables: 2,
        annotations: 2,
    };
};

// ============================================================
// EXPORTS
// ============================================================

module.exports = {
    // Dashboard generators
    generateDashboard,
    generateDevDashboard,
    generateProdDashboard,

    // Exporters
    dashboardExporters,

    // Panel generators (for custom dashboards)
    panels: DashboardPanelGenerator,

    // Metrics
    getDashboardMetrics,

    // Algorithm access
    promQLOptimizer,
    adaptiveRefresh,
};