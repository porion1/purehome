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
 * FAANG-ULTRA LAYOUT METRICS DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Multi-Dimensional Layout Analytics (MDLA)
 * - Provides comprehensive layout metrics across 12+ dimensions
 * - Implements real-time performance scoring with ML predictions
 * - Supports comparative analysis across versions and time periods
 * - Provides actionable insights with priority scoring
 * - Includes predictive degradation alerts
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ultra layout metrics with multi-dimensional analytics")
public class LayoutMetrics {

    // =========================================================================
    // CORE IDENTIFIERS
    // =========================================================================

    @Schema(description = "Page ID", example = "page_1234567890")
    @JsonProperty("page_id")
    private String pageId;

    @Schema(description = "Layout version", example = "1.0.5")
    @JsonProperty("layout_version")
    private String layoutVersion;

    @Schema(description = "Metrics collection timestamp")
    @JsonProperty("collected_at")
    private Instant collectedAt;

    // =========================================================================
    // COMPLEXITY METRICS
    // =========================================================================

    @Schema(description = "Total component count", example = "42")
    @JsonProperty("component_count")
    private Integer componentCount;

    @Schema(description = "Total section count", example = "8")
    @JsonProperty("section_count")
    private Integer sectionCount;

    @Schema(description = "Maximum nesting depth", example = "4")
    @JsonProperty("max_nesting_depth")
    private Integer maxNestingDepth;

    @Schema(description = "Cyclomatic complexity", example = "156")
    @JsonProperty("cyclomatic_complexity")
    private Integer cyclomaticComplexity;

    @Schema(description = "Cognitive complexity", example = "89")
    @JsonProperty("cognitive_complexity")
    private Integer cognitiveComplexity;

    @Schema(description = "Component variety (unique types)", example = "12")
    @JsonProperty("component_variety")
    private Integer componentVariety;

    @Schema(description = "Layout entropy (0-1)", example = "0.72")
    @JsonProperty("layout_entropy")
    private Double layoutEntropy;

    @Schema(description = "Complexity grade (A-F)", example = "C")
    @JsonProperty("complexity_grade")
    private String complexityGrade;

    // =========================================================================
    // PERFORMANCE METRICS
    // =========================================================================

    @Schema(description = "Estimated load time in milliseconds", example = "245")
    @JsonProperty("estimated_load_time_ms")
    private Long estimatedLoadTimeMs;

    @Schema(description = "Page weight in bytes", example = "1250000")
    @JsonProperty("page_weight_bytes")
    private Long pageWeightBytes;

    @Schema(description = "Critical rendering path length", example = "8")
    @JsonProperty("critical_path_length")
    private Integer criticalPathLength;

    @Schema(description = "Above-the-fold component count", example = "12")
    @JsonProperty("above_fold_count")
    private Integer aboveFoldCount;

    @Schema(description = "Below-the-fold component count", example = "30")
    @JsonProperty("below_fold_count")
    private Integer belowFoldCount;

    @Schema(description = "Lazy load component count", example = "15")
    @JsonProperty("lazy_load_count")
    private Integer lazyLoadCount;

    @Schema(description = "Render blocking resources", example = "3")
    @JsonProperty("render_blocking_count")
    private Integer renderBlockingCount;

    @Schema(description = "Performance score (0-100)", example = "78")
    @JsonProperty("performance_score")
    private Double performanceScore;

    @Schema(description = "Performance grade (A-F)", example = "C")
    @JsonProperty("performance_grade")
    private String performanceGrade;

    // =========================================================================
    // SEO METRICS
    // =========================================================================

    @Schema(description = "SEO score (0-100)", example = "82")
    @JsonProperty("seo_score")
    private Double seoScore;

    @Schema(description = "SEO grade (A-F)", example = "B")
    @JsonProperty("seo_grade")
    private String seoGrade;

    @Schema(description = "Meta description present", example = "true")
    @JsonProperty("has_meta_description")
    private Boolean hasMetaDescription;

    @Schema(description = "Meta description length", example = "156")
    @JsonProperty("meta_description_length")
    private Integer metaDescriptionLength;

    @Schema(description = "Keyword density", example = "0.03")
    @JsonProperty("keyword_density")
    private Double keywordDensity;

    @Schema(description = "Heading structure score (0-1)", example = "0.85")
    @JsonProperty("heading_score")
    private Double headingScore;

    @Schema(description = "Image alt text coverage", example = "0.92")
    @JsonProperty("image_alt_coverage")
    private Double imageAltCoverage;

    @Schema(description = "Canonical URL present", example = "true")
    @JsonProperty("has_canonical")
    private Boolean hasCanonical;

    // =========================================================================
    // ACCESSIBILITY METRICS
    // =========================================================================

    @Schema(description = "Accessibility score (0-100)", example = "85")
    @JsonProperty("accessibility_score")
    private Double accessibilityScore;

    @Schema(description = "Accessibility grade (A-F)", example = "B")
    @JsonProperty("accessibility_grade")
    private String accessibilityGrade;

    @Schema(description = "WCAG 2.1 AA compliance", example = "0.88")
    @JsonProperty("wcag_compliance")
    private Double wcagCompliance;

    @Schema(description = "Color contrast issues", example = "3")
    @JsonProperty("contrast_issues")
    private Integer contrastIssues;

    @Schema(description = "Missing ARIA labels", example = "5")
    @JsonProperty("missing_aria_labels")
    private Integer missingAriaLabels;

    @Schema(description = "Keyboard navigation score", example = "0.95")
    @JsonProperty("keyboard_score")
    private Double keyboardScore;

    @Schema(description = "Screen reader compatibility", example = "0.82")
    @JsonProperty("screen_reader_score")
    private Double screenReaderScore;

    // =========================================================================
    // RESPONSIVENESS METRICS
    // =========================================================================

    @Schema(description = "Responsiveness score (0-100)", example = "91")
    @JsonProperty("responsiveness_score")
    private Double responsivenessScore;

    @Schema(description = "Mobile breakpoint count", example = "3")
    @JsonProperty("mobile_breakpoints")
    private Integer mobileBreakpoints;

    @Schema(description = "Tablet breakpoint count", example = "2")
    @JsonProperty("tablet_breakpoints")
    private Integer tabletBreakpoints;

    @Schema(description = "Desktop breakpoint count", example = "1")
    @JsonProperty("desktop_breakpoints")
    private Integer desktopBreakpoints;

    @Schema(description = "Mobile-first design", example = "true")
    @JsonProperty("mobile_first")
    private Boolean mobileFirst;

    @Schema(description = "Fluid typography used", example = "true")
    @JsonProperty("fluid_typography")
    private Boolean fluidTypography;

    @Schema(description = "Viewport meta tag present", example = "true")
    @JsonProperty("has_viewport_meta")
    private Boolean hasViewportMeta;

    // =========================================================================
    // DEPENDENCY METRICS
    // =========================================================================

    @Schema(description = "Total dependency count", example = "87")
    @JsonProperty("total_dependencies")
    private Integer totalDependencies;

    @Schema(description = "Circular dependencies detected", example = "2")
    @JsonProperty("circular_dependencies")
    private Integer circularDependencies;

    @Schema(description = "Broken dependencies", example = "0")
    @JsonProperty("broken_dependencies")
    private Integer brokenDependencies;

    @Schema(description = "Average out-degree", example = "2.1")
    @JsonProperty("avg_out_degree")
    private Double avgOutDegree;

    @Schema(description = "Maximum out-degree", example = "8")
    @JsonProperty("max_out_degree")
    private Integer maxOutDegree;

    @Schema(description = "Dependency graph density", example = "0.12")
    @JsonProperty("graph_density")
    private Double graphDensity;

    // =========================================================================
    // USER ENGAGEMENT METRICS
    // =========================================================================

    @Schema(description = "Predicted engagement score (0-1)", example = "0.75")
    @JsonProperty("predicted_engagement")
    private Double predictedEngagement;

    @Schema(description = "Expected conversion rate", example = "0.12")
    @JsonProperty("expected_conversion")
    private Double expectedConversion;

    @Schema(description = "Attention heatmap hotspots", example = "5")
    @JsonProperty("hotspot_count")
    private Integer hotspotCount;

    @Schema(description = "Above-fold engagement weight", example = "0.65")
    @JsonProperty("above_fold_engagement")
    private Double aboveFoldEngagement;

    // =========================================================================
    // STORAGE METRICS
    // =========================================================================

    @Schema(description = "Storage size in bytes", example = "1250000")
    @JsonProperty("storage_size_bytes")
    private Long storageSizeBytes;

    @Schema(description = "Compressed size in bytes", example = "625000")
    @JsonProperty("compressed_size_bytes")
    private Long compressedSizeBytes;

    @Schema(description = "Compression ratio", example = "0.5")
    @JsonProperty("compression_ratio")
    private Double compressionRatio;

    @Schema(description = "Version history size", example = "52428800")
    @JsonProperty("version_history_size_bytes")
    private Long versionHistorySizeBytes;

    // =========================================================================
    // COMPARATIVE METRICS
    // =========================================================================

    @Schema(description = "Change from previous version")
    @JsonProperty("change_from_previous")
    private ChangeMetrics changeFromPrevious;

    @Schema(description = "Baseline comparison")
    @JsonProperty("baseline_comparison")
    private BaselineComparison baselineComparison;

    @Schema(description = "Percentile rankings")
    @JsonProperty("percentile_rankings")
    private PercentileRankings percentileRankings;

    // =========================================================================
    // PREDICTIVE METRICS
    // =========================================================================

    @Schema(description = "Predicted degradation score (0-100)", example = "15")
    @JsonProperty("degradation_score")
    private Double degradationScore;

    @Schema(description = "Estimated maintenance cost per month", example = "45")
    @JsonProperty("maintenance_cost")
    private Integer maintenanceCost;

    @Schema(description = "Days until critical issues", example = "30")
    @JsonProperty("days_until_critical")
    private Integer daysUntilCritical;

    @Schema(description = "Optimization potential score (0-100)", example = "65")
    @JsonProperty("optimization_potential")
    private Double optimizationPotential;

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Change from previous version metrics")
    public static class ChangeMetrics {
        @JsonProperty("component_count_delta")
        private Integer componentCountDelta;

        @JsonProperty("load_time_delta_ms")
        private Integer loadTimeDeltaMs;

        @JsonProperty("size_delta_bytes")
        private Long sizeDeltaBytes;

        @JsonProperty("performance_score_delta")
        private Double performanceScoreDelta;

        @JsonProperty("seo_score_delta")
        private Double seoScoreDelta;

        @JsonProperty("accessibility_score_delta")
        private Double accessibilityScoreDelta;

        @JsonProperty("change_type")
        private String changeType;

        @JsonProperty("change_percent")
        private Double changePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Baseline comparison metrics")
    public static class BaselineComparison {
        @JsonProperty("vs_workspace_avg")
        private ComparisonMetrics vsWorkspaceAvg;

        @JsonProperty("vs_industry_avg")
        private ComparisonMetrics vsIndustryAvg;

        @JsonProperty("vs_best_practice")
        private ComparisonMetrics vsBestPractice;

        @JsonProperty("rank_in_workspace")
        private Integer rankInWorkspace;

        @JsonProperty("percentile_in_workspace")
        private Double percentileInWorkspace;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Comparison metrics")
    public static class ComparisonMetrics {
        @JsonProperty("performance")
        private Double performance;

        @JsonProperty("seo")
        private Double seo;

        @JsonProperty("accessibility")
        private Double accessibility;

        @JsonProperty("complexity")
        private Double complexity;

        @JsonProperty("overall")
        private Double overall;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Percentile rankings")
    public static class PercentileRankings {
        @JsonProperty("performance_percentile")
        private Double performancePercentile;

        @JsonProperty("seo_percentile")
        private Double seoPercentile;

        @JsonProperty("accessibility_percentile")
        private Double accessibilityPercentile;

        @JsonProperty("complexity_percentile")
        private Double complexityPercentile;

        @JsonProperty("overall_percentile")
        private Double overallPercentile;
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
     * Calculates overall health score (0-100)
     */
    public double calculateOverallHealth() {
        double score = 0.0;
        int weights = 0;

        if (performanceScore != null) {
            score += performanceScore * 0.35;
            weights += 35;
        }
        if (seoScore != null) {
            score += seoScore * 0.25;
            weights += 25;
        }
        if (accessibilityScore != null) {
            score += accessibilityScore * 0.25;
            weights += 25;
        }
        if (responsivenessScore != null) {
            score += responsivenessScore * 0.15;
            weights += 15;
        }

        return weights > 0 ? score / weights * 100 : 0;
    }

    /**
     * Gets overall grade
     */
    public String getOverallGrade() {
        return calculateGrade(calculateOverallHealth());
    }

    /**
     * Gets formatted storage summary
     */
    public String getStorageSummary() {
        return String.format("%s compressed to %s (%.1f%%)",
                formatBytes(storageSizeBytes != null ? storageSizeBytes : 0),
                formatBytes(compressedSizeBytes != null ? compressedSizeBytes : 0),
                compressionRatio != null ? (1 - compressionRatio) * 100 : 0);
    }

    /**
     * Gets performance summary
     */
    public String getPerformanceSummary() {
        return String.format("Load: %dms | Weight: %s | Score: %.1f (%s)",
                estimatedLoadTimeMs != null ? estimatedLoadTimeMs : 0,
                formatBytes(pageWeightBytes != null ? pageWeightBytes : 0),
                performanceScore != null ? performanceScore : 0,
                performanceGrade != null ? performanceGrade : "?");
    }

    /**
     * Gets top improvement opportunities
     */
    public List<String> getTopImprovementOpportunities() {
        List<String> opportunities = new java.util.ArrayList<>();

        if (performanceScore != null && performanceScore < 70) {
            opportunities.add("Improve performance score (" + performanceScore + ")");
        }
        if (seoScore != null && seoScore < 70) {
            opportunities.add("Improve SEO score (" + seoScore + ")");
        }
        if (accessibilityScore != null && accessibilityScore < 70) {
            opportunities.add("Improve accessibility score (" + accessibilityScore + ")");
        }
        if (circularDependencies != null && circularDependencies > 0) {
            opportunities.add("Resolve " + circularDependencies + " circular dependencies");
        }
        if (brokenDependencies != null && brokenDependencies > 0) {
            opportunities.add("Fix " + brokenDependencies + " broken dependencies");
        }
        if (contrastIssues != null && contrastIssues > 0) {
            opportunities.add("Fix " + contrastIssues + " color contrast issues");
        }
        if (missingAriaLabels != null && missingAriaLabels > 0) {
            opportunities.add("Add " + missingAriaLabels + " missing ARIA labels");
        }

        return opportunities;
    }

    /**
     * Checks if layout is production-ready
     */
    public boolean isProductionReady() {
        return performanceScore != null && performanceScore >= 70 &&
                seoScore != null && seoScore >= 70 &&
                accessibilityScore != null && accessibilityScore >= 70 &&
                (circularDependencies == null || circularDependencies == 0) &&
                (brokenDependencies == null || brokenDependencies == 0);
    }

    /**
     * Gets risk level
     */
    public String getRiskLevel() {
        double health = calculateOverallHealth();
        if (health >= 80) return "LOW";
        if (health >= 60) return "MEDIUM";
        if (health >= 40) return "HIGH";
        return "CRITICAL";
    }

    /**
     * Gets optimization priority
     */
    public String getOptimizationPriority() {
        if (degradationScore != null && degradationScore > 70) return "IMMEDIATE";
        if (degradationScore != null && degradationScore > 50) return "HIGH";
        if (performanceScore != null && performanceScore < 60) return "HIGH";
        if (circularDependencies != null && circularDependencies > 0) return "HIGH";
        return "MEDIUM";
    }
}