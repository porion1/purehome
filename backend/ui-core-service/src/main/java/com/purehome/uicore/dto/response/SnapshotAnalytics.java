package com.purehome.uicore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * FAANG-ULTRA SNAPSHOT ANALYTICS DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Temporal Snapshot Intelligence (TSI)
 * - Provides comprehensive analytics across snapshot history
 * - Implements trend analysis with ML-based forecasting
 * - Supports compliance reporting with audit trails
 * - Provides storage optimization recommendations
 * - Includes snapshot health scoring and anomaly detection
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ultra snapshot analytics with temporal intelligence")
public class SnapshotAnalytics {

    // =========================================================================
    // CORE IDENTIFIERS
    // =========================================================================

    @Schema(description = "Workspace ID", example = "workspace_123")
    @JsonProperty("workspace_id")
    private String workspaceId;

    @Schema(description = "Page ID (if page-specific)", example = "page_456")
    @JsonProperty("page_id")
    private String pageId;

    @Schema(description = "Analysis period start")
    @JsonProperty("period_start")
    private Instant periodStart;

    @Schema(description = "Analysis period end")
    @JsonProperty("period_end")
    private Instant periodEnd;

    @Schema(description = "Analysis timestamp")
    @JsonProperty("analyzed_at")
    private Instant analyzedAt;

    // =========================================================================
    // VOLUME METRICS
    // =========================================================================

    @Schema(description = "Total snapshots", example = "156")
    @JsonProperty("total_snapshots")
    private Integer totalSnapshots;

    @Schema(description = "Snapshots by type")
    @JsonProperty("snapshots_by_type")
    private Map<String, Integer> snapshotsByType;

    @Schema(description = "Snapshots by storage tier")
    @JsonProperty("snapshots_by_tier")
    private Map<String, Integer> snapshotsByTier;

    @Schema(description = "Average snapshots per day", example = "2.3")
    @JsonProperty("avg_snapshots_per_day")
    private Double avgSnapshotsPerDay;

    @Schema(description = "Peak snapshot day", example = "2024-01-15")
    @JsonProperty("peak_snapshot_day")
    private String peakSnapshotDay;

    @Schema(description = "Peak snapshot count", example = "12")
    @JsonProperty("peak_snapshot_count")
    private Integer peakSnapshotCount;

    // =========================================================================
    // STORAGE METRICS
    // =========================================================================

    @Schema(description = "Total storage used in bytes", example = "524288000")
    @JsonProperty("total_storage_bytes")
    private Long totalStorageBytes;

    @Schema(description = "Storage by tier in bytes")
    @JsonProperty("storage_by_tier_bytes")
    private Map<String, Long> storageByTierBytes;

    @Schema(description = "Storage by type in bytes")
    @JsonProperty("storage_by_type_bytes")
    private Map<String, Long> storageByTypeBytes;

    @Schema(description = "Average snapshot size in bytes", example = "3355443")
    @JsonProperty("avg_snapshot_size_bytes")
    private Long avgSnapshotSizeBytes;

    @Schema(description = "Largest snapshot ID", example = "snapshot_001")
    @JsonProperty("largest_snapshot_id")
    private String largestSnapshotId;

    @Schema(description = "Largest snapshot size in bytes", example = "10485760")
    @JsonProperty("largest_snapshot_size_bytes")
    private Long largestSnapshotSizeBytes;

    @Schema(description = "Compression savings in bytes", example = "262144000")
    @JsonProperty("compression_savings_bytes")
    private Long compressionSavingsBytes;

    @Schema(description = "Compression ratio", example = "0.5")
    @JsonProperty("compression_ratio")
    private Double compressionRatio;

    // =========================================================================
    // RETENTION METRICS
    // =========================================================================

    @Schema(description = "Snapshots expiring in next 7 days", example = "5")
    @JsonProperty("expiring_7days")
    private Integer expiring7Days;

    @Schema(description = "Snapshots expiring in next 30 days", example = "23")
    @JsonProperty("expiring_30days")
    private Integer expiring30Days;

    @Schema(description = "Expired snapshots", example = "12")
    @JsonProperty("expired_snapshots")
    private Integer expiredSnapshots;

    @Schema(description = "Average retention days", example = "45.5")
    @JsonProperty("avg_retention_days")
    private Double avgRetentionDays;

    @Schema(description = "Snapshots past retention", example = "8")
    @JsonProperty("past_retention_count")
    private Integer pastRetentionCount;

    @Schema(description = "Storage reclaimable by cleanup in bytes", example = "52428800")
    @JsonProperty("reclaimable_bytes")
    private Long reclaimableBytes;

    // =========================================================================
    // ACTIVITY METRICS
    // =========================================================================

    @Schema(description = "Snapshot frequency timeline")
    @JsonProperty("frequency_timeline")
    private List<FrequencyPoint> frequencyTimeline;

    @Schema(description = "Most active users")
    @JsonProperty("top_users")
    private List<UserActivity> topUsers;

    @Schema(description = "Most active pages")
    @JsonProperty("top_pages")
    private List<PageActivity> topPages;

    @Schema(description = "Snapshot creation trends")
    @JsonProperty("trends")
    private TrendAnalysis trends;

    // =========================================================================
    // HEALTH METRICS
    // =========================================================================

    @Schema(description = "Snapshot health score (0-100)", example = "92.5")
    @JsonProperty("health_score")
    private Double healthScore;

    @Schema(description = "Health grade (A-F)", example = "A")
    @JsonProperty("health_grade")
    private String healthGrade;

    @Schema(description = "Integrity issues found", example = "0")
    @JsonProperty("integrity_issues")
    private Integer integrityIssues;

    @Schema(description = "Corrupted snapshots", example = "0")
    @JsonProperty("corrupted_snapshots")
    private Integer corruptedSnapshots;

    @Schema(description = "Orphaned snapshots", example = "2")
    @JsonProperty("orphaned_snapshots")
    private Integer orphanedSnapshots;

    @Schema(description = "Snapshots with missing chain", example = "1")
    @JsonProperty("missing_chain")
    private Integer missingChain;

    // =========================================================================
    // COST METRICS
    // =========================================================================

    @Schema(description = "Estimated monthly storage cost (USD)", example = "12.50")
    @JsonProperty("monthly_cost_usd")
    private Double monthlyCostUsd;

    @Schema(description = "Cost by storage tier")
    @JsonProperty("cost_by_tier_usd")
    private Map<String, Double> costByTierUsd;

    @Schema(description = "Projected cost next month (USD)", example = "13.75")
    @JsonProperty("projected_cost_usd")
    private Double projectedCostUsd;

    @Schema(description = "Cost savings recommendations")
    @JsonProperty("cost_savings_recommendations")
    private List<CostRecommendation> costSavingsRecommendations;

    // =========================================================================
    // COMPLIANCE METRICS
    // =========================================================================

    @Schema(description = "GDPR compliance status", example = "COMPLIANT")
    @JsonProperty("gdpr_status")
    private String gdprStatus;

    @Schema(description = "SOC2 compliance status", example = "COMPLIANT")
    @JsonProperty("soc2_status")
    private String soc2Status;

    @Schema(description = "Data retention policy adherence", example = "0.98")
    @JsonProperty("retention_adherence")
    private Double retentionAdherence;

    @Schema(description = "Audit trail completeness", example = "0.99")
    @JsonProperty("audit_completeness")
    private Double auditCompleteness;

    // =========================================================================
    // PREDICTIVE METRICS
    // =========================================================================

    @Schema(description = "Predicted snapshot growth rate", example = "0.15")
    @JsonProperty("predicted_growth_rate")
    private Double predictedGrowthRate;

    @Schema(description = "Predicted storage in 30 days (bytes)", example = "629145600")
    @JsonProperty("predicted_storage_30d_bytes")
    private Long predictedStorage30dBytes;

    @Schema(description = "Predicted snapshot count in 30 days", example = "180")
    @JsonProperty("predicted_count_30d")
    private Integer predictedCount30d;

    @Schema(description = "Anomaly detection alerts")
    @JsonProperty("anomaly_alerts")
    private List<AnomalyAlert> anomalyAlerts;

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Snapshot frequency timeline point")
    public static class FrequencyPoint {
        @JsonProperty("date")
        private String date;

        @JsonProperty("count")
        private Integer count;

        @JsonProperty("total_size_bytes")
        private Long totalSizeBytes;

        @JsonProperty("unique_users")
        private Integer uniqueUsers;

        @JsonProperty("unique_pages")
        private Integer uniquePages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User activity summary")
    public static class UserActivity {
        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("snapshot_count")
        private Integer snapshotCount;

        @JsonProperty("total_size_bytes")
        private Long totalSizeBytes;

        @JsonProperty("first_snapshot")
        private Instant firstSnapshot;

        @JsonProperty("last_snapshot")
        private Instant lastSnapshot;

        @JsonProperty("snapshot_types")
        private Map<String, Integer> snapshotTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Page activity summary")
    public static class PageActivity {
        @JsonProperty("page_id")
        private String pageId;

        @JsonProperty("page_title")
        private String pageTitle;

        @JsonProperty("snapshot_count")
        private Integer snapshotCount;

        @JsonProperty("total_size_bytes")
        private Long totalSizeBytes;

        @JsonProperty("latest_version")
        private String latestVersion;

        @JsonProperty("latest_snapshot")
        private Instant latestSnapshot;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Trend analysis")
    public static class TrendAnalysis {
        @JsonProperty("snapshot_trend")
        private String snapshotTrend;

        @JsonProperty("storage_trend")
        private String storageTrend;

        @JsonProperty("growth_rate")
        private Double growthRate;

        @JsonProperty("acceleration")
        private Double acceleration;

        @JsonProperty("seasonality_factors")
        private Map<String, Double> seasonalityFactors;

        @JsonProperty("forecast_confidence")
        private Double forecastConfidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Cost recommendation")
    public static class CostRecommendation {
        @JsonProperty("type")
        private String type;

        @JsonProperty("description")
        private String description;

        @JsonProperty("estimated_savings_usd")
        private Double estimatedSavingsUsd;

        @JsonProperty("implementation_effort")
        private String implementationEffort;

        @JsonProperty("priority")
        private String priority;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Anomaly alert")
    public static class AnomalyAlert {
        @JsonProperty("alert_id")
        private String alertId;

        @JsonProperty("type")
        private String type;

        @JsonProperty("description")
        private String description;

        @JsonProperty("severity")
        private String severity;

        @JsonProperty("detected_at")
        private Instant detectedAt;

        @JsonProperty("affected_snapshots")
        private List<String> affectedSnapshots;

        @JsonProperty("recommendation")
        private String recommendation;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String calculateGrade(double score) {
        if (score >= 95) return "A+";
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    /**
     * Gets formatted storage summary
     */
    public String getStorageSummary() {
        return String.format("Total: %s | Avg: %s | Savings: %s (%.1f%%)",
                formatBytes(totalStorageBytes != null ? totalStorageBytes : 0),
                formatBytes(avgSnapshotSizeBytes != null ? avgSnapshotSizeBytes : 0),
                formatBytes(compressionSavingsBytes != null ? compressionSavingsBytes : 0),
                compressionRatio != null ? compressionRatio * 100 : 0);
    }

    /**
     * Gets formatted cost summary
     */
    public String getCostSummary() {
        return String.format("Current: $%.2f/mo | Projected: $%.2f/mo | Potential Savings: $%.2f/mo",
                monthlyCostUsd != null ? monthlyCostUsd : 0,
                projectedCostUsd != null ? projectedCostUsd : 0,
                costSavingsRecommendations != null ?
                        costSavingsRecommendations.stream()
                                .mapToDouble(r -> r.getEstimatedSavingsUsd() != null ? r.getEstimatedSavingsUsd() : 0)
                                .sum() : 0);
    }

    /**
     * Gets formatted health summary
     */
    public String getHealthSummary() {
        return String.format("Score: %.1f (%s) | Issues: %d integrity, %d corrupted, %d orphaned",
                healthScore != null ? healthScore : 0,
                healthGrade != null ? healthGrade : "?",
                integrityIssues != null ? integrityIssues : 0,
                corruptedSnapshots != null ? corruptedSnapshots : 0,
                orphanedSnapshots != null ? orphanedSnapshots : 0);
    }

    /**
     * Gets formatted compliance summary
     */
    public String getComplianceSummary() {
        return String.format("GDPR: %s | SOC2: %s | Retention: %.1f%% | Audit: %.1f%%",
                gdprStatus != null ? gdprStatus : "UNKNOWN",
                soc2Status != null ? soc2Status : "UNKNOWN",
                retentionAdherence != null ? retentionAdherence * 100 : 0,
                auditCompleteness != null ? auditCompleteness * 100 : 0);
    }

    /**
     * Gets cleanup recommendations
     */
    public List<String> getCleanupRecommendations() {
        List<String> recommendations = new java.util.ArrayList<>();

        if (expiredSnapshots != null && expiredSnapshots > 0) {
            recommendations.add("Delete " + expiredSnapshots + " expired snapshots to reclaim " +
                    formatBytes(reclaimableBytes != null ? reclaimableBytes : 0));
        }

        if (pastRetentionCount != null && pastRetentionCount > 0) {
            recommendations.add("Review " + pastRetentionCount + " snapshots past retention policy");
        }

        if (orphanedSnapshots != null && orphanedSnapshots > 0) {
            recommendations.add("Resolve " + orphanedSnapshots + " orphaned snapshots");
        }

        if (missingChain != null && missingChain > 0) {
            recommendations.add("Repair version chain for " + missingChain + " snapshots");
        }

        return recommendations;
    }

    /**
     * Gets storage optimization recommendations
     */
    public List<String> getStorageOptimizations() {
        List<String> optimizations = new java.util.ArrayList<>();

        if (compressionRatio != null && compressionRatio < 0.3) {
            optimizations.add("Enable compression to reduce storage by up to 70%");
        }

        if (storageByTierBytes != null) {
            long hotBytes = storageByTierBytes.getOrDefault("HOT", 0L);
            long warmBytes = storageByTierBytes.getOrDefault("WARM", 0L);

            if (hotBytes > warmBytes * 10) {
                optimizations.add("Move older snapshots to WARM tier to reduce costs");
            }
        }

        if (avgSnapshotSizeBytes != null && avgSnapshotSizeBytes > 10 * 1024 * 1024) {
            optimizations.add("Enable delta compression for large snapshots");
        }

        return optimizations;
    }

    /**
     * Gets retention policy recommendations
     */
    public List<String> getRetentionRecommendations() {
        List<String> recommendations = new java.util.ArrayList<>();

        if (avgRetentionDays != null && avgRetentionDays > 365) {
            recommendations.add("Consider reducing retention period from " +
                    Math.round(avgRetentionDays) + " days to 90 days to save " +
                    formatBytes(reclaimableBytes != null ? reclaimableBytes : 0));
        }

        Map<String, Integer> manualSnapshots = snapshotsByType != null ?
                Map.of("MANUAL", snapshotsByType.getOrDefault("MANUAL", 0)) : Map.of();

        if (manualSnapshots.getOrDefault("MANUAL", 0) > 100) {
            recommendations.add("High number of manual snapshots. Consider automated schedules.");
        }

        return recommendations;
    }

    /**
     * Gets overall analytics summary
     */
    public String getSummary() {
        return String.format("""
                === SNAPSHOT ANALYTICS SUMMARY ===
                Period: %s to %s
                Total Snapshots: %d
                Storage: %s
                Health Score: %.1f (%s)
                Monthly Cost: $%.2f
                Compliance: %s
                Recommendations: %d
                """,
                periodStart != null ? periodStart : "N/A",
                periodEnd != null ? periodEnd : "N/A",
                totalSnapshots != null ? totalSnapshots : 0,
                getStorageSummary(),
                healthScore != null ? healthScore : 0,
                healthGrade != null ? healthGrade : "?",
                monthlyCostUsd != null ? monthlyCostUsd : 0,
                getComplianceSummary(),
                getCleanupRecommendations().size() + getStorageOptimizations().size() +
                        getRetentionRecommendations().size());
    }
}