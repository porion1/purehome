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
 * FAANG-ULTRA ADD COMPONENT RESPONSE DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Intelligent Component Placement Acknowledgment (ICPA)
 * - Provides atomic component creation confirmation
 * - Includes smart suggestions for related components
 * - Provides dependency graph updates for connected components
 * - Supports A/B test variant tracking and analytics
 * - Includes performance impact analysis
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ultra add component response with intelligent placement acknowledgment")
public class AddComponentResponse {

    // =========================================================================
    // OPERATION STATUS
    // =========================================================================

    @Schema(description = "Whether component was added successfully", example = "true")
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Operation ID", example = "add_comp_abc123")
    @JsonProperty("operation_id")
    private String operationId;

    @Schema(description = "Response message", example = "Component added successfully")
    @JsonProperty("message")
    private String message;

    // =========================================================================
    // COMPONENT DATA
    // =========================================================================

    @Schema(description = "Created component")
    @JsonProperty("component")
    private PageLayout.LayoutComponent component;

    @Schema(description = "Component ID", example = "comp_1234567890")
    @JsonProperty("component_id")
    private String componentId;

    @Schema(description = "Component type", example = "HERO")
    @JsonProperty("component_type")
    private String componentType;

    @Schema(description = "Section ID where component was added", example = "section_content")
    @JsonProperty("section_id")
    private String sectionId;

    @Schema(description = "Position index in section", example = "3")
    @JsonProperty("position")
    private Integer position;

    @Schema(description = "Grid position (if applicable)")
    @JsonProperty("grid_position")
    private GridPosition gridPosition;

    // =========================================================================
    // UPDATED LAYOUT
    // =========================================================================

    @Schema(description = "Complete updated layout")
    @JsonProperty("updated_layout")
    private PageLayout updatedLayout;

    @Schema(description = "Layout diff for incremental updates")
    @JsonProperty("layout_diff")
    private Map<String, Object> layoutDiff;

    @Schema(description = "Affected components from addition")
    @JsonProperty("affected_components")
    private List<AffectedComponent> affectedComponents;

    // =========================================================================
    // DEPENDENCY MANAGEMENT
    // =========================================================================

    @Schema(description = "Dependencies that were created")
    @JsonProperty("created_dependencies")
    private List<DependencyInfo> createdDependencies;

    @Schema(description = "Dependencies that were updated")
    @JsonProperty("updated_dependencies")
    private List<DependencyInfo> updatedDependencies;

    @Schema(description = "Missing dependencies that need resolution")
    @JsonProperty("missing_dependencies")
    private List<MissingDependency> missingDependencies;

    @Schema(description = "Auto-resolved dependencies")
    @JsonProperty("auto_resolved")
    private List<AutoResolvedDependency> autoResolved;

    // =========================================================================
    // SMART SUGGESTIONS
    // =========================================================================

    @Schema(description = "Suggested related components to add")
    @JsonProperty("suggested_components")
    private List<ComponentSuggestion> suggestedComponents;

    @Schema(description = "Suggested optimizations")
    @JsonProperty("suggested_optimizations")
    private List<OptimizationSuggestion> suggestedOptimizations;

    @Schema(description = "Accessibility recommendations")
    @JsonProperty("accessibility_recommendations")
    private List<AccessibilityRecommendation> accessibilityRecommendations;

    // =========================================================================
    // A/B TESTING DATA
    // =========================================================================

    @Schema(description = "A/B test ID if applicable", example = "ab_test_hero_001")
    @JsonProperty("ab_test_id")
    private String abTestId;

    @Schema(description = "Variant ID", example = "variant_b")
    @JsonProperty("variant_id")
    private String variantId;

    @Schema(description = "Control group component ID for comparison")
    @JsonProperty("control_component_id")
    private String controlComponentId;

    @Schema(description = "Expected lift metrics")
    @JsonProperty("expected_lift")
    private ExpectedLift expectedLift;

    // =========================================================================
    // PERFORMANCE IMPACT
    // =========================================================================

    @Schema(description = "Performance impact of new component")
    @JsonProperty("performance_impact")
    private PerformanceImpact performanceImpact;

    @Schema(description = "Page weight after addition in bytes", example = "1250000")
    @JsonProperty("page_weight_bytes")
    private Long pageWeightBytes;

    @Schema(description = "Estimated load time impact in milliseconds", example = "+45")
    @JsonProperty("load_time_impact_ms")
    private Integer loadTimeImpactMs;

    @Schema(description = "Above-the-fold impact", example = "No impact")
    @JsonProperty("above_fold_impact")
    private String aboveFoldImpact;

    // =========================================================================
    // VALIDATION RESULTS
    // =========================================================================

    @Schema(description = "Validation results after addition")
    @JsonProperty("validation")
    private LayoutValidationResponse validation;

    @Schema(description = "Warnings that don't block operation")
    @JsonProperty("warnings")
    private List<String> warnings;

    @Schema(description = "Information messages")
    @JsonProperty("info")
    private List<String> info;

    // =========================================================================
    // VERSION CONTROL
    // =========================================================================

    @Schema(description = "New version vector")
    @JsonProperty("new_version_vector")
    private String newVersionVector;

    @Schema(description = "New layout version", example = "1.0.6")
    @JsonProperty("layout_version")
    private String layoutVersion;

    @Schema(description = "Layout hash for integrity verification")
    @JsonProperty("layout_hash")
    private String layoutHash;

    // =========================================================================
    // METRICS
    // =========================================================================

    @Schema(description = "Operation duration in milliseconds", example = "156")
    @JsonProperty("duration_ms")
    private Long durationMs;

    @Schema(description = "Timestamp")
    @JsonProperty("timestamp")
    private Instant timestamp;

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Grid position")
    public static class GridPosition {
        @JsonProperty("x")
        private int x;
        @JsonProperty("y")
        private int y;
        @JsonProperty("width")
        private int width;
        @JsonProperty("height")
        private int height;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Affected component")
    public static class AffectedComponent {
        @JsonProperty("component_id")
        private String componentId;
        @JsonProperty("previous_position")
        private Integer previousPosition;
        @JsonProperty("new_position")
        private Integer newPosition;
        @JsonProperty("change_type")
        private String changeType;
        @JsonProperty("reason")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Dependency information")
    public static class DependencyInfo {
        @JsonProperty("source_component_id")
        private String sourceComponentId;
        @JsonProperty("target_component_id")
        private String targetComponentId;
        @JsonProperty("dependency_type")
        private String dependencyType;
        @JsonProperty("strength")
        private Double strength;
        @JsonProperty("auto_created")
        private Boolean autoCreated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Missing dependency")
    public static class MissingDependency {
        @JsonProperty("component_id")
        private String componentId;
        @JsonProperty("missing_reference")
        private String missingReference;
        @JsonProperty("reference_type")
        private String referenceType;
        @JsonProperty("suggestion")
        private String suggestion;
        @JsonProperty("severity")
        private String severity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Auto-resolved dependency")
    public static class AutoResolvedDependency {
        @JsonProperty("component_id")
        private String componentId;
        @JsonProperty("resolved_reference")
        private String resolvedReference;
        @JsonProperty("resolution_strategy")
        private String resolutionStrategy;
        @JsonProperty("confidence")
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Component suggestion")
    public static class ComponentSuggestion {
        @JsonProperty("type")
        private String type;
        @JsonProperty("name")
        private String name;
        @JsonProperty("description")
        private String description;
        @JsonProperty("reason")
        private String reason;
        @JsonProperty("priority")
        private String priority;
        @JsonProperty("estimated_impact")
        private String estimatedImpact;
        @JsonProperty("add_url")
        private String addUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Optimization suggestion")
    public static class OptimizationSuggestion {
        @JsonProperty("type")
        private String type;
        @JsonProperty("message")
        private String message;
        @JsonProperty("impact")
        private String impact;
        @JsonProperty("effort")
        private String effort;
        @JsonProperty("auto_fixable")
        private Boolean autoFixable;
        @JsonProperty("auto_fix_action")
        private String autoFixAction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Accessibility recommendation")
    public static class AccessibilityRecommendation {
        @JsonProperty("guideline")
        private String guideline;
        @JsonProperty("message")
        private String message;
        @JsonProperty("component_property")
        private String componentProperty;
        @JsonProperty("suggested_value")
        private String suggestedValue;
        @JsonProperty("impact")
        private String impact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Expected lift metrics")
    public static class ExpectedLift {
        @JsonProperty("engagement")
        private Double engagement;
        @JsonProperty("conversion")
        private Double conversion;
        @JsonProperty("revenue")
        private Double revenue;
        @JsonProperty("confidence")
        private Double confidence;
        @JsonProperty("sample_size_needed")
        private Integer sampleSizeNeeded;
        @JsonProperty("test_duration_days")
        private Integer testDurationDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Performance impact")
    public static class PerformanceImpact {
        @JsonProperty("load_time_ms")
        private Integer loadTimeMs;
        @JsonProperty("weight_bytes")
        private Long weightBytes;
        @JsonProperty("render_blocking")
        private Boolean renderBlocking;
        @JsonProperty("critical_path")
        private Boolean criticalPath;
        @JsonProperty("optimization_tips")
        private List<String> optimizationTips;
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Creates a success response for component addition
     */
    public static AddComponentResponse success(PageLayout.LayoutComponent component,
                                               String sectionId, int position,
                                               PageLayout updatedLayout,
                                               String newVersionVector,
                                               long durationMs) {
        return AddComponentResponse.builder()
                .success(true)
                .message("Component added successfully")
                .component(component)
                .componentId(component.getId())
                .componentType(component.getType())
                .sectionId(sectionId)
                .position(position)
                .updatedLayout(updatedLayout)
                .newVersionVector(newVersionVector)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a success response for grid-positioned component
     */
    public static AddComponentResponse successWithGrid(PageLayout.LayoutComponent component,
                                                       String sectionId, GridPosition gridPosition,
                                                       PageLayout updatedLayout,
                                                       String newVersionVector,
                                                       long durationMs) {
        return AddComponentResponse.builder()
                .success(true)
                .message("Component added to grid position")
                .component(component)
                .componentId(component.getId())
                .componentType(component.getType())
                .sectionId(sectionId)
                .gridPosition(gridPosition)
                .updatedLayout(updatedLayout)
                .newVersionVector(newVersionVector)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a response with dependency information
     */
    public static AddComponentResponse withDependencies(AddComponentResponse response,
                                                        List<DependencyInfo> createdDependencies,
                                                        List<MissingDependency> missingDependencies,
                                                        List<AutoResolvedDependency> autoResolved) {
        response.setCreatedDependencies(createdDependencies);
        response.setMissingDependencies(missingDependencies);
        response.setAutoResolved(autoResolved);
        return response;
    }

    /**
     * Creates a response with smart suggestions
     */
    public static AddComponentResponse withSuggestions(AddComponentResponse response,
                                                       List<ComponentSuggestion> suggestions,
                                                       List<OptimizationSuggestion> optimizations) {
        response.setSuggestedComponents(suggestions);
        response.setSuggestedOptimizations(optimizations);
        return response;
    }

    /**
     * Creates an A/B test variant response
     */
    public static AddComponentResponse abTestVariant(PageLayout.LayoutComponent component,
                                                     String sectionId, int position,
                                                     String abTestId, String variantId,
                                                     PageLayout updatedLayout,
                                                     String newVersionVector,
                                                     ExpectedLift expectedLift,
                                                     long durationMs) {
        return AddComponentResponse.builder()
                .success(true)
                .message("A/B test variant added successfully")
                .component(component)
                .componentId(component.getId())
                .componentType(component.getType())
                .sectionId(sectionId)
                .position(position)
                .updatedLayout(updatedLayout)
                .abTestId(abTestId)
                .variantId(variantId)
                .expectedLift(expectedLift)
                .newVersionVector(newVersionVector)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a validation failure response
     */
    public static AddComponentResponse validationFailure(String componentType,
                                                         LayoutValidationResponse validation,
                                                         List<String> warnings,
                                                         long durationMs) {
        return AddComponentResponse.builder()
                .success(false)
                .message("Component validation failed")
                .componentType(componentType)
                .validation(validation)
                .warnings(warnings)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Gets the component summary string
     */
    public String getComponentSummary() {
        if (component == null) return "No component data";

        return String.format("%s component (%s) at position %d in section %s",
                component.getType(),
                component.getId(),
                position != null ? position : (gridPosition != null ? "grid " + gridPosition.x + "," + gridPosition.y : "unknown"),
                sectionId);
    }

    /**
     * Checks if there are any missing dependencies
     */
    public boolean hasMissingDependencies() {
        return missingDependencies != null && !missingDependencies.isEmpty();
    }

    /**
     * Gets critical missing dependencies
     */
    public List<MissingDependency> getCriticalMissingDependencies() {
        if (missingDependencies == null) return List.of();
        return missingDependencies.stream()
                .filter(d -> "CRITICAL".equals(d.getSeverity()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Gets the performance grade
     */
    public String getPerformanceGrade() {
        if (performanceImpact == null || performanceImpact.getLoadTimeMs() == null) {
            return "UNKNOWN";
        }
        int loadTime = performanceImpact.getLoadTimeMs();
        if (loadTime < 50) return "A+";
        if (loadTime < 100) return "A";
        if (loadTime < 200) return "B";
        if (loadTime < 500) return "C";
        if (loadTime < 1000) return "D";
        return "F";
    }

    /**
     * Gets a human-readable status message
     */
    public String getStatusMessage() {
        if (!success) {
            return "❌ Failed: " + message;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("✅ Added ").append(componentType).append(" component");

        if (abTestId != null) {
            sb.append(" for A/B test ").append(abTestId);
        }

        if (hasMissingDependencies()) {
            sb.append(" ⚠️ ").append(missingDependencies.size()).append(" missing dependencies");
        }

        if (suggestedComponents != null && !suggestedComponents.isEmpty()) {
            sb.append(" 💡 ").append(suggestedComponents.size()).append(" suggestions available");
        }

        return sb.toString();
    }
}