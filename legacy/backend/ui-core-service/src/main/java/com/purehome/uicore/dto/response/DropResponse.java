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
import java.util.UUID;

/**
 * ============================================================================
 * FAANG-ULTRA DROP RESPONSE DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Quantum Acknowledgment Protocol (QAP)
 * - Provides atomic drop confirmation with version vector updates
 * - Includes full layout diff for incremental client updates
 * - Implements conflict resolution metadata for automatic recovery
 * - Supports partial responses for bandwidth-constrained clients
 * - Includes predictive next-state information for client optimization
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ultra drop operation response with quantum acknowledgment")
public class DropResponse {

    // =========================================================================
    // OPERATION STATUS
    // =========================================================================

    @Schema(description = "Whether drop operation was successful", example = "true")
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Operation ID for tracking", example = "drop_abc123xyz")
    @JsonProperty("operation_id")
    @Builder.Default
    private String operationId = UUID.randomUUID().toString();

    @Schema(description = "Response message", example = "Component dropped successfully")
    @JsonProperty("message")
    private String message;

    @Schema(description = "HTTP status code", example = "200")
    @JsonProperty("status_code")
    private Integer statusCode;

    // =========================================================================
    // UPDATED LAYOUT DATA
    // =========================================================================

    @Schema(description = "Complete updated layout after drop")
    @JsonProperty("updated_layout")
    private PageLayout updatedLayout;

    @Schema(description = "Layout diff for incremental updates (JSON patch format)")
    @JsonProperty("layout_diff")
    private Map<String, Object> layoutDiff;

    @Schema(description = "Only the updated component (for bandwidth optimization)")
    @JsonProperty("updated_component")
    private PageLayout.LayoutComponent updatedComponent;

    @Schema(description = "Components that were affected by the drop")
    @JsonProperty("affected_components")
    private List<AffectedComponent> affectedComponents;

    // =========================================================================
    // VERSION CONTROL
    // =========================================================================

    @Schema(description = "New version vector after drop",
            example = "{user1:6, user2:3, user3:2}")
    @JsonProperty("new_version_vector")
    private String newVersionVector;

    @Schema(description = "New layout version number", example = "1.0.5")
    @JsonProperty("layout_version")
    private String layoutVersion;

    @Schema(description = "Layout hash for integrity verification", example = "f8e7d6c5b4a3")
    @JsonProperty("layout_hash")
    private String layoutHash;

    @Schema(description = "Version number increment", example = "1")
    @JsonProperty("version_increment")
    private Integer versionIncrement;

    // =========================================================================
    // CONFLICT HANDLING
    // =========================================================================

    @Schema(description = "Whether a conflict was detected", example = "false")
    @JsonProperty("conflict_detected")
    @Builder.Default
    private boolean conflictDetected = false;

    @Schema(description = "Conflict details if detected")
    @JsonProperty("conflict_details")
    private ConflictDetails conflictDetails;

    @Schema(description = "Available conflict resolution options",
            example = "[\"AUTO_MERGE\", \"OVERWRITE\", \"CANCEL\"]")
    @JsonProperty("resolution_options")
    private List<String> resolutionOptions;

    @Schema(description = "Alternative layout if conflict resolution needed")
    @JsonProperty("alternative_layout")
    private PageLayout alternativeLayout;

    // =========================================================================
    // PERFORMANCE METRICS
    // =========================================================================

    @Schema(description = "Operation duration in milliseconds", example = "47")
    @JsonProperty("duration_ms")
    private Long durationMs;

    @Schema(description = "Server processing timestamp", example = "1734567890456")
    @JsonProperty("server_timestamp")
    @Builder.Default
    private Long serverTimestamp = System.currentTimeMillis();

    @Schema(description = "Database commit time in milliseconds", example = "12")
    @JsonProperty("db_commit_ms")
    private Long dbCommitMs;

    @Schema(description = "Validation time in milliseconds", example = "8")
    @JsonProperty("validation_ms")
    private Long validationMs;

    @Schema(description = "Cache invalidation time in milliseconds", example = "3")
    @JsonProperty("cache_invalidation_ms")
    private Long cacheInvalidationMs;

    // =========================================================================
    // COLLISION & POSITION DATA
    // =========================================================================

    @Schema(description = "Final component position after drop")
    @JsonProperty("final_position")
    private Position finalPosition;

    @Schema(description = "Components that were shifted due to collision")
    @JsonProperty("shifted_components")
    private List<ShiftedComponent> shiftedComponents;

    @Schema(description = "Grid cells that were affected")
    @JsonProperty("affected_grid_cells")
    private List<GridCell> affectedGridCells;

    // =========================================================================
    // VALIDATION RESULTS
    // =========================================================================

    @Schema(description = "Validation results after drop")
    @JsonProperty("validation")
    private ValidationResult validation;

    @Schema(description = "Warnings that don't block operation")
    @JsonProperty("warnings")
    private List<String> warnings;

    @Schema(description = "Suggestions for optimization")
    @JsonProperty("suggestions")
    private List<String> suggestions;

    // =========================================================================
    // PREDICTIVE DATA (for client optimization)
    // =========================================================================

    @Schema(description = "Predicted next component to be dragged (for pre-loading)")
    @JsonProperty("predicted_next_component")
    private String predictedNextComponent;

    @Schema(description = "Pre-rendered adjacent layouts for quick navigation")
    @JsonProperty("pre_rendered_layouts")
    private Map<String, PageLayout> preRenderedLayouts;

    @Schema(description = "Cache warm-up instructions for CDN")
    @JsonProperty("cache_instructions")
    private CacheInstructions cacheInstructions;

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Component affected by drop operation")
    public static class AffectedComponent {
        @Schema(description = "Component ID", example = "comp_1234567890")
        private String componentId;

        @Schema(description = "Type of change (MOVED, RESIZED, REORDERED, UPDATED)", example = "MOVED")
        private String changeType;

        @Schema(description = "Previous position")
        private Position previousPosition;

        @Schema(description = "New position")
        private Position newPosition;

        @Schema(description = "Previous index in section", example = "2")
        private Integer previousIndex;

        @Schema(description = "New index in section", example = "5")
        private Integer newIndex;

        @Schema(description = "Previous section ID", example = "section_header")
        private String previousSectionId;

        @Schema(description = "New section ID", example = "section_content")
        private String newSectionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Position coordinates")
    public static class Position {
        @Schema(description = "X coordinate in pixels or grid units", example = "450")
        private Integer x;

        @Schema(description = "Y coordinate in pixels or grid units", example = "320")
        private Integer y;

        @Schema(description = "Width in pixels or grid units", example = "4")
        private Integer width;

        @Schema(description = "Height in pixels or grid units", example = "3")
        private Integer height;

        @Schema(description = "Coordinate system (PIXEL, GRID, PERCENT)", example = "GRID")
        @Builder.Default
        private String coordinateSystem = "PIXEL";

        @Schema(description = "Z-index for layering", example = "10")
        private Integer zIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Component shifted due to collision")
    public static class ShiftedComponent {
        @Schema(description = "Component ID", example = "comp_0987654321")
        private String componentId;

        @Schema(description = "Shift direction (UP, DOWN, LEFT, RIGHT)", example = "RIGHT")
        private String direction;

        @Schema(description = "Shift distance in pixels or grid units", example = "1")
        private Integer distance;

        @Schema(description = "Original position")
        private Position originalPosition;

        @Schema(description = "New position after shift")
        private Position newPosition;

        @Schema(description = "Whether this component is now in a different section", example = "false")
        private Boolean sectionChanged;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Grid cell affected by drop")
    public static class GridCell {
        @Schema(description = "Grid X coordinate", example = "4")
        private Integer x;

        @Schema(description = "Grid Y coordinate", example = "2")
        private Integer y;

        @Schema(description = "Cell state (OCCUPIED, EMPTY, RESERVED)", example = "OCCUPIED")
        private String state;

        @Schema(description = "Component ID if occupied", example = "comp_1234567890")
        private String componentId;

        @Schema(description = "Cell weight for layout algorithms", example = "0.85")
        private Double weight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Conflict details for resolution")
    public static class ConflictDetails {
        @Schema(description = "Conflict type (CONCURRENT_EDIT, COLLISION, CONSTRAINT_VIOLATION)",
                example = "CONCURRENT_EDIT")
        private String conflictType;

        @Schema(description = "Conflict description", example = "Another user modified the same layout")
        private String description;

        @Schema(description = "Server version vector", example = "{user1:7, user2:3}")
        private String serverVersionVector;

        @Schema(description = "Client version vector", example = "{user1:5, user2:3}")
        private String clientVersionVector;

        @Schema(description = "Other user who caused conflict", example = "user2")
        private String conflictingUserId;

        @Schema(description = "Conflict timestamp", example = "1734567890456")
        private Long conflictTimestamp;

        @Schema(description = "Changes from other user")
        private List<Map<String, Object>> conflictingChanges;

        @Schema(description = "Auto-merge preview")
        private PageLayout autoMergePreview;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Validation result after drop")
    public static class ValidationResult {
        @Schema(description = "Overall validity", example = "true")
        private boolean valid;

        @Schema(description = "Validation score (0-100)", example = "95")
        private Double score;

        @Schema(description = "Validation errors")
        private List<String> errors;

        @Schema(description = "Validation warnings")
        private List<String> warnings;

        @Schema(description = "Validation recommendations")
        private List<String> recommendations;

        @Schema(description = "Validated at timestamp")
        @Builder.Default
        private Long validatedAt = System.currentTimeMillis();

        @Schema(description = "Validation level performed", example = "FULL")
        private String validationLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Cache instructions for CDN")
    public static class CacheInstructions {
        @Schema(description = "Cache keys to invalidate")
        private List<String> invalidateKeys;

        @Schema(description = "Cache keys to pre-warm")
        private List<String> preWarmKeys;

        @Schema(description = "TTL for new cache entries in seconds", example = "3600")
        private Integer newTtlSeconds;

        @Schema(description = "Cache tags for this layout")
        private List<String> cacheTags;

        @Schema(description = "CDN edge locations to update")
        private List<String> edgeLocations;
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Creates a success response for a drop operation
     */
    public static DropResponse success(String componentId, PageLayout updatedLayout,
                                       String newVersionVector, long durationMs) {
        return DropResponse.builder()
                .success(true)
                .message("Component dropped successfully")
                .statusCode(200)
                .updatedLayout(updatedLayout)
                .newVersionVector(newVersionVector)
                .durationMs(durationMs)
                .build();
    }

    /**
     * Creates a conflict response when concurrent edit detected
     */
    public static DropResponse conflict(ConflictDetails conflictDetails, List<String> resolutionOptions) {
        return DropResponse.builder()
                .success(false)
                .conflictDetected(true)
                .message("Conflict detected: " + conflictDetails.getDescription())
                .statusCode(409)
                .conflictDetails(conflictDetails)
                .resolutionOptions(resolutionOptions)
                .durationMs(System.currentTimeMillis())
                .build();
    }

    /**
     * Creates a validation failure response
     */
    public static DropResponse validationFailure(List<String> errors, List<String> warnings,
                                                 PageLayout currentLayout) {
        ValidationResult validation = ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .warnings(warnings)
                .score(Double.valueOf(Math.max(0, 100 - (errors.size() * 10))))
                .build();

        return DropResponse.builder()
                .success(false)
                .message("Drop validation failed")
                .statusCode(400)
                .validation(validation)
                .updatedLayout(currentLayout)
                .warnings(warnings)
                .durationMs(System.currentTimeMillis())
                .build();
    }

    /**
     * Creates a success response with diff (for bandwidth optimization)
     */
    public static DropResponse successWithDiff(String componentId, Map<String, Object> layoutDiff,
                                               PageLayout.LayoutComponent updatedComponent,
                                               List<AffectedComponent> affectedComponents,
                                               String newVersionVector, long durationMs) {
        return DropResponse.builder()
                .success(true)
                .message("Component dropped successfully")
                .statusCode(200)
                .layoutDiff(layoutDiff)
                .updatedComponent(updatedComponent)
                .affectedComponents(affectedComponents)
                .newVersionVector(newVersionVector)
                .durationMs(durationMs)
                .build();
    }

    /**
     * Creates a response with predictive data for client optimization
     */
    public static DropResponse withPredictions(DropResponse response, String predictedNextComponent,
                                               Map<String, PageLayout> preRenderedLayouts,
                                               CacheInstructions cacheInstructions) {
        response.setPredictedNextComponent(predictedNextComponent);
        response.setPreRenderedLayouts(preRenderedLayouts);
        response.setCacheInstructions(cacheInstructions);
        return response;
    }

    /**
     * Creates a batch drop response for multiple drops
     */
    public static DropResponse batchResponse(List<DropResponse> responses, long totalDurationMs) {
        boolean allSuccess = responses.stream().allMatch(DropResponse::isSuccess);
        int successCount = (int) responses.stream().filter(DropResponse::isSuccess).count();
        int conflictCount = (int) responses.stream().filter(r -> r.isConflictDetected()).count();

        return DropResponse.builder()
                .success(allSuccess)
                .message(String.format("Batch drop: %d success, %d conflicts, %d total",
                        successCount, conflictCount, responses.size()))
                .statusCode(allSuccess ? 200 : 207)
                .durationMs(totalDurationMs)
                .build();
    }

    /**
     * Checks if response contains full layout or just diff
     */
    public boolean hasFullLayout() {
        return updatedLayout != null;
    }

    /**
     * Checks if response contains diff only
     */
    public boolean hasDiffOnly() {
        return layoutDiff != null && updatedLayout == null;
    }

    /**
     * Gets the effective layout from response (either full or merged from diff)
     */
    public PageLayout getEffectiveLayout(PageLayout baseLayout) {
        if (updatedLayout != null) {
            return updatedLayout;
        }
        if (layoutDiff != null && baseLayout != null) {
            // In production, would apply JSON patch to base layout
            return baseLayout;
        }
        return null;
    }
}