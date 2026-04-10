package com.purehome.uicore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.purehome.uicore.model.PageLayout;
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
 * FAANG-ULTRA BATCH REORDER RESPONSE DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Quantum Annealing Result Aggregator (QARA)
 * - Aggregates optimization results from quantum annealing process
 * - Provides Pareto frontier visualization for trade-off analysis
 * - Includes confidence scores for each optimization dimension
 * - Supports incremental optimization with partial results
 * - Provides A/B testing metadata for optimization validation
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ultra batch reorder response with quantum annealing results")
public class BatchReorderResponse {

    // =========================================================================
    // OPERATION STATUS
    // =========================================================================

    @Schema(description = "Whether reorder was successful", example = "true")
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Operation ID for tracking", example = "batch_abc123xyz")
    @JsonProperty("operation_id")
    private String operationId;

    @Schema(description = "Response message", example = "Batch reorder completed successfully")
    @JsonProperty("message")
    private String message;

    // =========================================================================
    // UPDATED LAYOUT DATA
    // =========================================================================

    @Schema(description = "Complete updated layout after reorder")
    @JsonProperty("updated_layout")
    private PageLayout updatedLayout;

    @Schema(description = "Layout diff for incremental updates")
    @JsonProperty("layout_diff")
    private Map<String, Object> layoutDiff;

    @Schema(description = "Section that was reordered")
    @JsonProperty("section_id")
    private String sectionId;

    @Schema(description = "New component order after reorder")
    @JsonProperty("new_order")
    private List<String> newOrder;

    @Schema(description = "Components that were affected by reorder")
    @JsonProperty("affected_components")
    private List<ReorderAffectedComponent> affectedComponents;

    // =========================================================================
    // OPTIMIZATION METRICS
    // =========================================================================

    @Schema(description = "Whether optimization was applied", example = "true")
    @JsonProperty("optimized")
    private Boolean optimized;

    @Schema(description = "Optimization strategy used", example = "ENGAGEMENT")
    @JsonProperty("optimization_strategy")
    private String optimizationStrategy;

    @Schema(description = "Optimization score (0-100)", example = "87.5")
    @JsonProperty("optimization_score")
    private Double optimizationScore;

    @Schema(description = "Optimization improvement metrics")
    @JsonProperty("improvement_metrics")
    private ImprovementMetrics improvementMetrics;

    @Schema(description = "Pareto frontier solutions")
    @JsonProperty("pareto_frontier")
    private List<ParetoSolution> paretoFrontier;

    @Schema(description = "Optimization confidence (0-1)", example = "0.95")
    @JsonProperty("confidence")
    private Double confidence;

    // =========================================================================
    // QUANTUM ANNEALING METRICS
    // =========================================================================

    @Schema(description = "Number of annealing iterations performed", example = "10000")
    @JsonProperty("iterations_performed")
    private Integer iterationsPerformed;

    @Schema(description = "Best energy achieved", example = "1250.5")
    @JsonProperty("best_energy")
    private Double bestEnergy;

    @Schema(description = "Energy convergence curve")
    @JsonProperty("convergence_curve")
    private List<Double> convergenceCurve;

    @Schema(description = "Annealing duration in milliseconds", example = "234")
    @JsonProperty("annealing_duration_ms")
    private Long annealingDurationMs;

    @Schema(description = "Final temperature", example = "0.007")
    @JsonProperty("final_temperature")
    private Double finalTemperature;

    // =========================================================================
    // CONSTRAINT SATISFACTION
    // =========================================================================

    @Schema(description = "Constraints satisfied count", example = "15")
    @JsonProperty("constraints_satisfied")
    private Integer constraintsSatisfied;

    @Schema(description = "Constraints violated count", example = "1")
    @JsonProperty("constraints_violated")
    private Integer constraintsViolated;

    @Schema(description = "Constraint satisfaction details")
    @JsonProperty("constraint_details")
    private List<ConstraintDetail> constraintDetails;

    @Schema(description = "Constraint relaxation applied", example = "0.1")
    @JsonProperty("constraint_relaxation_applied")
    private Double constraintRelaxationApplied;

    // =========================================================================
    // PERFORMANCE IMPACT
    // =========================================================================

    @Schema(description = "Performance impact metrics")
    @JsonProperty("performance_impact")
    private PerformanceImpact performanceImpact;

    @Schema(description = "Estimated load time after reorder in milliseconds", example = "185")
    @JsonProperty("estimated_load_time_ms")
    private Long estimatedLoadTimeMs;

    @Schema(description = "Page weight after reorder in bytes", example = "950000")
    @JsonProperty("page_weight_bytes")
    private Long pageWeightBytes;

    @Schema(description = "Above-the-fold component count", example = "8")
    @JsonProperty("above_fold_count")
    private Integer aboveFoldCount;

    // =========================================================================
    // A/B TESTING METADATA
    // =========================================================================

    @Schema(description = "A/B test ID if applicable", example = "ab_test_001")
    @JsonProperty("ab_test_id")
    private String abTestId;

    @Schema(description = "Variant ID", example = "variant_b")
    @JsonProperty("variant_id")
    private String variantId;

    @Schema(description = "Control layout hash for comparison")
    @JsonProperty("control_hash")
    private String controlHash;

    @Schema(description = "Expected lift metrics")
    @JsonProperty("expected_lift")
    private ExpectedLift expectedLift;

    // =========================================================================
    // VALIDATION RESULTS
    // =========================================================================

    @Schema(description = "Validation results after reorder")
    @JsonProperty("validation")
    private LayoutValidationResponse validation;

    @Schema(description = "Warnings that don't block operation")
    @JsonProperty("warnings")
    private List<String> warnings;

    @Schema(description = "Suggestions for further optimization")
    @JsonProperty("suggestions")
    private List<String> suggestions;

    // =========================================================================
    // VERSION CONTROL
    // =========================================================================

    @Schema(description = "New version vector after reorder")
    @JsonProperty("new_version_vector")
    private String newVersionVector;

    @Schema(description = "New layout version", example = "1.0.6")
    @JsonProperty("layout_version")
    private String layoutVersion;

    @Schema(description = "Layout hash for integrity verification")
    @JsonProperty("layout_hash")
    private String layoutHash;

    // =========================================================================
    // TIMESTAMP
    // =========================================================================

    @Schema(description = "Operation timestamp")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Schema(description = "Operation duration in milliseconds", example = "312")
    @JsonProperty("duration_ms")
    private Long durationMs;

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Component affected by reorder")
    public static class ReorderAffectedComponent {
        @Schema(description = "Component ID", example = "comp_123")
        @JsonProperty("component_id")
        private String componentId;

        @Schema(description = "Previous index", example = "2")
        @JsonProperty("previous_index")
        private Integer previousIndex;

        @Schema(description = "New index", example = "5")
        @JsonProperty("new_index")
        private Integer newIndex;

        @Schema(description = "Position change delta", example = "3")
        @JsonProperty("delta")
        private Integer delta;

        @Schema(description = "Was component moved up", example = "false")
        @JsonProperty("moved_up")
        private Boolean movedUp;

        @Schema(description = "Was component moved down", example = "true")
        @JsonProperty("moved_down")
        private Boolean movedDown;

        @Schema(description = "Reason for movement", example = "OPTIMIZATION")
        @JsonProperty("reason")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Improvement metrics from optimization")
    public static class ImprovementMetrics {
        @Schema(description = "Expected engagement improvement (0-1)", example = "0.12")
        @JsonProperty("engagement_improvement")
        private Double engagementImprovement;

        @Schema(description = "Performance improvement in milliseconds", example = "-45")
        @JsonProperty("performance_improvement_ms")
        private Integer performanceImprovementMs;

        @Schema(description = "SEO score improvement", example = "8")
        @JsonProperty("seo_improvement")
        private Integer seoImprovement;

        @Schema(description = "Accessibility score improvement", example = "5")
        @JsonProperty("accessibility_improvement")
        private Integer accessibilityImprovement;

        @Schema(description = "Visual hierarchy improvement (0-1)", example = "0.08")
        @JsonProperty("hierarchy_improvement")
        private Double hierarchyImprovement;

        @Schema(description = "Overall improvement percentage", example = "15.3")
        @JsonProperty("overall_improvement_percent")
        private Double overallImprovementPercent;

        @Schema(description = "Confidence in improvements (0-1)", example = "0.92")
        @JsonProperty("confidence")
        private Double confidence;

        @Schema(description = "Statistical significance (0-1)", example = "0.95")
        @JsonProperty("significance")
        private Double significance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Pareto optimal solution")
    public static class ParetoSolution {
        @Schema(description = "Solution ID", example = "sol_001")
        @JsonProperty("id")
        private String id;

        @Schema(description = "Component order for this solution")
        @JsonProperty("order")
        private List<String> order;

        @Schema(description = "Engagement score (0-1)", example = "0.85")
        @JsonProperty("engagement_score")
        private Double engagementScore;

        @Schema(description = "Performance score (0-1)", example = "0.92")
        @JsonProperty("performance_score")
        private Double performanceScore;

        @Schema(description = "SEO score (0-1)", example = "0.78")
        @JsonProperty("seo_score")
        private Double seoScore;

        @Schema(description = "Accessibility score (0-1)", example = "0.88")
        @JsonProperty("accessibility_score")
        private Double accessibilityScore;

        @Schema(description = "Overall score (0-1)", example = "0.86")
        @JsonProperty("overall_score")
        private Double overallScore;

        @Schema(description = "Is this the recommended solution", example = "true")
        @JsonProperty("recommended")
        private Boolean recommended;

        @Schema(description = "Trade-off explanation")
        @JsonProperty("tradeoff")
        private String tradeoff;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Constraint satisfaction detail")
    public static class ConstraintDetail {
        @Schema(description = "Constraint name", example = "MAX_COMPONENTS_PER_SECTION")
        @JsonProperty("name")
        private String name;

        @Schema(description = "Constraint type (HARD, SOFT)", example = "HARD")
        @JsonProperty("type")
        private String type;

        @Schema(description = "Whether satisfied", example = "true")
        @JsonProperty("satisfied")
        private Boolean satisfied;

        @Schema(description = "Current value", example = "25")
        @JsonProperty("current_value")
        private Double currentValue;

        @Schema(description = "Required value", example = "<=20")
        @JsonProperty("required")
        private String required;

        @Schema(description = "If violated, violation magnitude", example = "5")
        @JsonProperty("violation_magnitude")
        private Double violationMagnitude;

        @Schema(description = "Fix suggestion if violated")
        @JsonProperty("fix_suggestion")
        private String fixSuggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Performance impact metrics")
    public static class PerformanceImpact {
        @Schema(description = "Before load time in milliseconds", example = "250")
        @JsonProperty("before_load_time_ms")
        private Long beforeLoadTimeMs;

        @Schema(description = "After load time in milliseconds", example = "185")
        @JsonProperty("after_load_time_ms")
        private Long afterLoadTimeMs;

        @Schema(description = "Load time improvement percentage", example = "26")
        @JsonProperty("load_time_improvement_percent")
        private Integer loadTimeImprovementPercent;

        @Schema(description = "Before page weight in bytes", example = "1250000")
        @JsonProperty("before_page_weight_bytes")
        private Long beforePageWeightBytes;

        @Schema(description = "After page weight in bytes", example = "950000")
        @JsonProperty("after_page_weight_bytes")
        private Long afterPageWeightBytes;

        @Schema(description = "Weight reduction percentage", example = "24")
        @JsonProperty("weight_reduction_percent")
        private Integer weightReductionPercent;

        @Schema(description = "Critical rendering path optimization", example = "Improved")
        @JsonProperty("critical_path")
        private String criticalPath;

        @Schema(description = "LCP improvement in milliseconds", example = "-120")
        @JsonProperty("lcp_improvement_ms")
        private Integer lcpImprovementMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Expected lift from A/B test")
    public static class ExpectedLift {
        @Schema(description = "Expected conversion lift percentage", example = "8.5")
        @JsonProperty("conversion_lift_percent")
        private Double conversionLiftPercent;

        @Schema(description = "Expected engagement lift percentage", example = "12.3")
        @JsonProperty("engagement_lift_percent")
        private Double engagementLiftPercent;

        @Schema(description = "Expected revenue lift percentage", example = "5.2")
        @JsonProperty("revenue_lift_percent")
        private Double revenueLiftPercent;

        @Schema(description = "Minimum detectable lift", example = "2.5")
        @JsonProperty("minimum_detectable_lift")
        private Double minimumDetectableLift;

        @Schema(description = "Sample size needed", example = "10000")
        @JsonProperty("sample_size_needed")
        private Integer sampleSizeNeeded;

        @Schema(description = "Test duration days", example = "14")
        @JsonProperty("test_duration_days")
        private Integer testDurationDays;

        @Schema(description = "Statistical power (0-1)", example = "0.8")
        @JsonProperty("power")
        private Double power;

        @Schema(description = "Significance level (0-1)", example = "0.05")
        @JsonProperty("significance_level")
        private Double significanceLevel;
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Creates a success response for simple reorder (no optimization)
     */
    public static BatchReorderResponse success(String sectionId, List<String> newOrder,
                                               PageLayout updatedLayout, String newVersionVector,
                                               long durationMs) {
        return BatchReorderResponse.builder()
                .success(true)
                .message("Batch reorder completed successfully")
                .sectionId(sectionId)
                .newOrder(newOrder)
                .updatedLayout(updatedLayout)
                .newVersionVector(newVersionVector)
                .optimized(false)
                .timestamp(Instant.now())
                .durationMs(durationMs)
                .build();
    }

    /**
     * Creates an optimized success response with metrics
     */
    public static BatchReorderResponse optimizedSuccess(String sectionId, List<String> newOrder,
                                                        PageLayout updatedLayout, String newVersionVector,
                                                        ImprovementMetrics metrics, PerformanceImpact impact,
                                                        double optimizationScore, long durationMs) {
        return BatchReorderResponse.builder()
                .success(true)
                .message("Optimized batch reorder completed successfully")
                .sectionId(sectionId)
                .newOrder(newOrder)
                .updatedLayout(updatedLayout)
                .newVersionVector(newVersionVector)
                .optimized(true)
                .optimizationScore(optimizationScore)
                .improvementMetrics(metrics)
                .performanceImpact(impact)
                .timestamp(Instant.now())
                .durationMs(durationMs)
                .build();
    }

    /**
     * Creates a response with Pareto frontier for user selection
     */
    public static BatchReorderResponse withParetoFrontier(String sectionId, List<String> currentOrder,
                                                          List<ParetoSolution> solutions, long durationMs) {
        ParetoSolution recommended = solutions.stream()
                .filter(ParetoSolution::getRecommended)
                .findFirst()
                .orElse(solutions.isEmpty() ? null : solutions.get(0));

        return BatchReorderResponse.builder()
                .success(true)
                .message("Multiple optimal solutions found. Select preferred trade-off.")
                .sectionId(sectionId)
                .newOrder(recommended != null ? recommended.getOrder() : currentOrder)
                .paretoFrontier(solutions)
                .optimized(true)
                .optimizationScore(recommended != null ? recommended.getOverallScore() * 100 : 0)
                .timestamp(Instant.now())
                .durationMs(durationMs)
                .build();
    }

    /**
     * Creates a validation failure response
     */
    public static BatchReorderResponse validationFailure(String sectionId, List<String> componentIds,
                                                         LayoutValidationResponse validation,
                                                         List<String> warnings, long durationMs) {
        return BatchReorderResponse.builder()
                .success(false)
                .message("Batch reorder validation failed")
                .sectionId(sectionId)
                .newOrder(componentIds)
                .validation(validation)
                .warnings(warnings)
                .timestamp(Instant.now())
                .durationMs(durationMs)
                .build();
    }

    /**
     * Creates a conflict response
     */
    public static BatchReorderResponse conflict(String sectionId, String versionVector,
                                                String newVersionVector, String message) {
        return BatchReorderResponse.builder()
                .success(false)
                .message(message != null ? message : "Concurrent modification detected")
                .sectionId(sectionId)
                .newVersionVector(newVersionVector)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Gets the optimization summary string
     */
    public String getOptimizationSummary() {
        if (!optimized || improvementMetrics == null) {
            return "No optimization applied";
        }

        return String.format(
                "Optimization applied: Engagement +%.1f%%, Performance +%.1f%%, SEO +%d, Accessibility +%d",
                improvementMetrics.getEngagementImprovement() != null ? improvementMetrics.getEngagementImprovement() * 100 : 0,
                improvementMetrics.getPerformanceImprovementMs() != null ? Math.abs(improvementMetrics.getPerformanceImprovementMs()) : 0,
                improvementMetrics.getSeoImprovement() != null ? improvementMetrics.getSeoImprovement() : 0,
                improvementMetrics.getAccessibilityImprovement() != null ? improvementMetrics.getAccessibilityImprovement() : 0
        );
    }

    /**
     * Checks if there were any constraint violations
     */
    public boolean hasConstraintViolations() {
        return constraintsViolated != null && constraintsViolated > 0;
    }

    /**
     * Gets the recommended solution from Pareto frontier
     */
    public ParetoSolution getRecommendedSolution() {
        if (paretoFrontier == null) return null;
        return paretoFrontier.stream()
                .filter(ParetoSolution::getRecommended)
                .findFirst()
                .orElse(paretoFrontier.isEmpty() ? null : paretoFrontier.get(0));
    }

    /**
     * Calculates the overall improvement percentage
     */
    public double getOverallImprovementPercent() {
        if (improvementMetrics != null && improvementMetrics.getOverallImprovementPercent() != null) {
            return improvementMetrics.getOverallImprovementPercent();
        }

        if (performanceImpact != null && performanceImpact.getLoadTimeImprovementPercent() != null) {
            return (double) performanceImpact.getLoadTimeImprovementPercent();
        }

        return 0;
    }

    /**
     * Gets a human-readable status message
     */
    public String getStatusMessage() {
        if (!success) {
            return "❌ Failed: " + message;
        }

        if (optimized && improvementMetrics != null) {
            return String.format("✅ Optimized: %s", getOptimizationSummary());
        }

        return "✅ Reordered " + (newOrder != null ? newOrder.size() : 0) + " components";
    }
}