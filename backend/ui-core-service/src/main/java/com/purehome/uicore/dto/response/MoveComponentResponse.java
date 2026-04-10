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
 * FAANG-ULTRA MOVE COMPONENT RESPONSE DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Hyperdimensional Vector Movement Acknowledgment (HVMA)
 * - Provides atomic move confirmation with full position tracking
 * - Includes dependency movement details for audit
 * - Supports incremental updates for large layouts
 * - Provides animation instructions for smooth transitions
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ultra move component response with hyperdimensional acknowledgment")
public class MoveComponentResponse {

    // =========================================================================
    // OPERATION STATUS
    // =========================================================================

    @Schema(description = "Whether move was successful", example = "true")
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Operation ID", example = "move_abc123")
    @JsonProperty("operation_id")
    private String operationId;

    @Schema(description = "Response message", example = "Component moved successfully")
    @JsonProperty("message")
    private String message;

    // =========================================================================
    // COMPONENT DATA
    // =========================================================================

    @Schema(description = "Component that was moved")
    @JsonProperty("component")
    private PageLayout.LayoutComponent component;

    @Schema(description = "Component ID", example = "comp_123")
    @JsonProperty("component_id")
    private String componentId;

    @Schema(description = "Source section ID", example = "section_header")
    @JsonProperty("source_section_id")
    private String sourceSectionId;

    @Schema(description = "Target section ID", example = "section_content")
    @JsonProperty("target_section_id")
    private String targetSectionId;

    @Schema(description = "New index in target section", example = "3")
    @JsonProperty("new_index")
    private Integer newIndex;

    @Schema(description = "New grid position (if applicable)")
    @JsonProperty("new_grid_position")
    private GridPosition newGridPosition;

    // =========================================================================
    // DEPENDENCY MOVEMENT
    // =========================================================================

    @Schema(description = "Dependencies that were moved with component")
    @JsonProperty("moved_dependencies")
    private List<MovedDependency> movedDependencies;

    @Schema(description = "Dependencies that were not moved")
    @JsonProperty("unmoved_dependencies")
    private List<String> unmovedDependencies;

    @Schema(description = "New dependencies created")
    @JsonProperty("new_dependencies")
    private List<DependencyInfo> newDependencies;

    @Schema(description = "Broken dependencies after move")
    @JsonProperty("broken_dependencies")
    private List<BrokenDependency> brokenDependencies;

    // =========================================================================
    // UPDATED LAYOUT
    // =========================================================================

    @Schema(description = "Complete updated layout")
    @JsonProperty("updated_layout")
    private PageLayout updatedLayout;

    @Schema(description = "Layout diff for incremental updates")
    @JsonProperty("layout_diff")
    private Map<String, Object> layoutDiff;

    @Schema(description = "Affected components from move")
    @JsonProperty("affected_components")
    private List<AffectedComponentInfo> affectedComponents;

    // =========================================================================
    // STYLE ADAPTATION
    // =========================================================================

    @Schema(description = "Styles that were adapted")
    @JsonProperty("adapted_styles")
    private Map<String, Object> adaptedStyles;

    @Schema(description = "Style changes applied")
    @JsonProperty("style_changes")
    private List<StyleChange> styleChanges;

    @Schema(description = "Responsive settings preserved", example = "true")
    @JsonProperty("responsive_preserved")
    private Boolean responsivePreserved;

    // =========================================================================
    // ANIMATION INSTRUCTIONS
    // =========================================================================

    @Schema(description = "Animation instructions for frontend")
    @JsonProperty("animation_instructions")
    private AnimationInstructions animationInstructions;

    // =========================================================================
    // VERSION CONTROL
    // =========================================================================

    @Schema(description = "New version vector")
    @JsonProperty("new_version_vector")
    private String newVersionVector;

    @Schema(description = "New layout version", example = "1.0.6")
    @JsonProperty("layout_version")
    private String layoutVersion;

    @Schema(description = "Layout hash")
    @JsonProperty("layout_hash")
    private String layoutHash;

    // =========================================================================
    // METRICS
    // =========================================================================

    @Schema(description = "Operation duration in milliseconds", example = "47")
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
        private Integer width;
        @JsonProperty("height")
        private Integer height;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Moved dependency")
    public static class MovedDependency {
        @JsonProperty("component_id")
        private String componentId;
        @JsonProperty("original_section")
        private String originalSection;
        @JsonProperty("new_section")
        private String newSection;
        @JsonProperty("new_index")
        private Integer newIndex;
        @JsonProperty("reason")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Dependency information")
    public static class DependencyInfo {
        @JsonProperty("component_id")
        private String componentId;
        @JsonProperty("dependency_type")
        private String dependencyType;
        @JsonProperty("strength")
        private Double strength;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Broken dependency")
    public static class BrokenDependency {
        @JsonProperty("component_id")
        private String componentId;
        @JsonProperty("depends_on")
        private String dependsOn;
        @JsonProperty("reason")
        private String reason;
        @JsonProperty("suggestion")
        private String suggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Affected component info")
    public static class AffectedComponentInfo {
        @JsonProperty("component_id")
        private String componentId;
        @JsonProperty("previous_index")
        private Integer previousIndex;
        @JsonProperty("new_index")
        private Integer newIndex;
        @JsonProperty("change_type")
        private String changeType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Style change")
    public static class StyleChange {
        @JsonProperty("property")
        private String property;
        @JsonProperty("old_value")
        private Object oldValue;
        @JsonProperty("new_value")
        private Object newValue;
        @JsonProperty("reason")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Animation instructions")
    public static class AnimationInstructions {
        @JsonProperty("duration_ms")
        private Integer durationMs;
        @JsonProperty("easing")
        private String easing;
        @JsonProperty("from_position")
        private GridPosition fromPosition;
        @JsonProperty("to_position")
        private GridPosition toPosition;
        @JsonProperty("scale")
        private Double scale;
        @JsonProperty("opacity")
        private Double opacity;
        @JsonProperty("rotate")
        private Integer rotate;
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    public static MoveComponentResponse success(String componentId, String sourceSection,
                                                String targetSection, int newIndex,
                                                PageLayout updatedLayout, String newVersionVector,
                                                long durationMs) {
        return MoveComponentResponse.builder()
                .success(true)
                .message("Component moved successfully")
                .componentId(componentId)
                .sourceSectionId(sourceSection)
                .targetSectionId(targetSection)
                .newIndex(newIndex)
                .updatedLayout(updatedLayout)
                .newVersionVector(newVersionVector)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
    }

    public static MoveComponentResponse successWithGrid(String componentId, String sourceSection,
                                                        String targetSection, GridPosition gridPosition,
                                                        PageLayout updatedLayout, String newVersionVector,
                                                        long durationMs) {
        return MoveComponentResponse.builder()
                .success(true)
                .message("Component moved to grid position")
                .componentId(componentId)
                .sourceSectionId(sourceSection)
                .targetSectionId(targetSection)
                .newGridPosition(gridPosition)
                .updatedLayout(updatedLayout)
                .newVersionVector(newVersionVector)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
    }

    public static MoveComponentResponse withDependencies(MoveComponentResponse response,
                                                         List<MovedDependency> movedDependencies,
                                                         List<BrokenDependency> brokenDependencies) {
        response.setMovedDependencies(movedDependencies);
        response.setBrokenDependencies(brokenDependencies);
        return response;
    }

    public static MoveComponentResponse withAnimation(MoveComponentResponse response,
                                                      AnimationInstructions animationInstructions) {
        response.setAnimationInstructions(animationInstructions);
        return response;
    }
}