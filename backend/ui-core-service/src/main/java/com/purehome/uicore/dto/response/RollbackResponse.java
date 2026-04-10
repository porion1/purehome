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
 * FAANG-ULTRA ROLLBACK RESPONSE DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Temporal Rollback with Dependency Preservation (TR-DP)
 * - Implements atomic rollback with full dependency graph preservation
 * - Provides preview mode for safe rollback testing
 * - Supports partial rollback with selective component restoration
 * - Includes rollback verification with integrity validation
 * - Provides rollback audit trail for compliance
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ultra rollback response with temporal dependency preservation")
public class RollbackResponse {

    // =========================================================================
    // OPERATION STATUS
    // =========================================================================

    @Schema(description = "Whether rollback was successful", example = "true")
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Rollback operation ID", example = "rollback_abc123")
    @JsonProperty("rollback_id")
    private String rollbackId;

    @Schema(description = "Response message", example = "Layout rolled back to version 1.0.5")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Rollback status (COMPLETED, PREVIEW, FAILED, PARTIAL)", example = "COMPLETED")
    @JsonProperty("status")
    private String status;

    // =========================================================================
    // ROLLBACK TARGET
    // =========================================================================

    @Schema(description = "Target snapshot ID rolled back to", example = "snapshot_123456")
    @JsonProperty("target_snapshot_id")
    private String targetSnapshotId;

    @Schema(description = "Target version number", example = "5")
    @JsonProperty("target_version")
    private Integer targetVersion;

    @Schema(description = "Target version string", example = "1.0.5")
    @JsonProperty("target_version_string")
    private String targetVersionString;

    @Schema(description = "Target snapshot timestamp", example = "2024-01-15T10:30:00Z")
    @JsonProperty("target_timestamp")
    private Instant targetTimestamp;

    @Schema(description = "Target snapshot label", example = "Before major redesign")
    @JsonProperty("target_label")
    private String targetLabel;

    // =========================================================================
    // RESTORED LAYOUT
    // =========================================================================

    @Schema(description = "Restored layout after rollback")
    @JsonProperty("restored_layout")
    private PageLayout restoredLayout;

    @Schema(description = "Layout hash for integrity verification")
    @JsonProperty("layout_hash")
    private String layoutHash;

    @Schema(description = "New layout version after rollback", example = "1.0.6")
    @JsonProperty("new_layout_version")
    private String newLayoutVersion;

    @Schema(description = "New version vector after rollback")
    @JsonProperty("new_version_vector")
    private String newVersionVector;

    // =========================================================================
    // CHANGE ANALYSIS
    // =========================================================================

    @Schema(description = "Summary of changes between current and restored layout")
    @JsonProperty("change_summary")
    private ChangeSummary changeSummary;

    @Schema(description = "Detailed list of changes applied")
    @JsonProperty("applied_changes")
    private List<AppliedChange> appliedChanges;

    @Schema(description = "Components that were preserved (not rolled back)")
    @JsonProperty("preserved_components")
    private List<String> preservedComponents;

    @Schema(description = "Components that were rolled back")
    @JsonProperty("rolled_back_components")
    private List<String> rolledBackComponents;

    @Schema(description = "Components that were newly created during rollback")
    @JsonProperty("new_components")
    private List<String> newComponents;

    // =========================================================================
    // DEPENDENCY PRESERVATION
    // =========================================================================

    @Schema(description = "Dependencies that were preserved")
    @JsonProperty("preserved_dependencies")
    private List<DependencyInfo> preservedDependencies;

    @Schema(description = "Dependencies that were broken")
    @JsonProperty("broken_dependencies")
    private List<BrokenDependency> brokenDependencies;

    @Schema(description = "Dependencies that were repaired")
    @JsonProperty("repaired_dependencies")
    private List<RepairedDependency> repairedDependencies;

    @Schema(description = "Orphaned components after rollback")
    @JsonProperty("orphaned_components")
    private List<String> orphanedComponents;

    // =========================================================================
    // CONFLICT RESOLUTION
    // =========================================================================

    @Schema(description = "Conflicts encountered during rollback")
    @JsonProperty("conflicts")
    private List<RollbackConflict> conflicts;

    @Schema(description = "Conflicts that were auto-resolved")
    @JsonProperty("auto_resolved_conflicts")
    private List<ResolvedConflict> autoResolvedConflicts;

    @Schema(description = "Conflicts requiring manual resolution")
    @JsonProperty("manual_conflicts")
    private List<ManualConflict> manualConflicts;

    // =========================================================================
    // VERIFICATION
    // =========================================================================

    @Schema(description = "Integrity verification result")
    @JsonProperty("integrity_verification")
    private IntegrityVerification integrityVerification;

    @Schema(description = "Validation result after rollback")
    @JsonProperty("validation")
    private LayoutValidationResponse validation;

    @Schema(description = "Rollback verification score (0-100)", example = "98.5")
    @JsonProperty("verification_score")
    private Double verificationScore;

    // =========================================================================
    // PREVIEW MODE DATA
    // =========================================================================

    @Schema(description = "Whether this was a preview rollback", example = "false")
    @JsonProperty("is_preview")
    private Boolean isPreview;

    @Schema(description = "Preview ID for committing preview rollback")
    @JsonProperty("preview_id")
    private String previewId;

    @Schema(description = "Preview expiration timestamp")
    @JsonProperty("preview_expires_at")
    private Instant previewExpiresAt;

    // =========================================================================
    // METRICS
    // =========================================================================

    @Schema(description = "Rollback duration in milliseconds", example = "234")
    @JsonProperty("duration_ms")
    private Long durationMs;

    @Schema(description = "Rollback timestamp")
    @JsonProperty("rolled_back_at")
    private Instant rolledBackAt;

    @Schema(description = "Rollback performed by user", example = "admin@purehome.com")
    @JsonProperty("rolled_back_by")
    private String rolledBackBy;

    @Schema(description = "Rollback reason", example = "Performance degradation after update")
    @JsonProperty("reason")
    private String reason;

    // =========================================================================
    // AUDIT & COMPLIANCE
    // =========================================================================

    @Schema(description = "Audit event ID for compliance tracking")
    @JsonProperty("audit_event_id")
    private String auditEventId;

    @Schema(description = "Compliance report URL")
    @JsonProperty("compliance_report_url")
    private String complianceReportUrl;

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Summary of changes during rollback")
    public static class ChangeSummary {
        @JsonProperty("components_added")
        private Integer componentsAdded;

        @JsonProperty("components_removed")
        private Integer componentsRemoved;

        @JsonProperty("components_modified")
        private Integer componentsModified;

        @JsonProperty("components_restored")
        private Integer componentsRestored;

        @JsonProperty("sections_added")
        private Integer sectionsAdded;

        @JsonProperty("sections_removed")
        private Integer sectionsRemoved;

        @JsonProperty("sections_modified")
        private Integer sectionsModified;

        @JsonProperty("total_changes")
        private Integer totalChanges;

        @JsonProperty("impact_score")
        private Double impactScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Applied change detail")
    public static class AppliedChange {
        @JsonProperty("component_id")
        private String componentId;

        @JsonProperty("change_type")
        private String changeType;

        @JsonProperty("field")
        private String field;

        @JsonProperty("old_value")
        private Object oldValue;

        @JsonProperty("new_value")
        private Object newValue;

        @JsonProperty("success")
        private Boolean success;

        @JsonProperty("error")
        private String error;
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

        @JsonProperty("severity")
        private String severity;

        @JsonProperty("suggestion")
        private String suggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Repaired dependency")
    public static class RepairedDependency {
        @JsonProperty("component_id")
        private String componentId;

        @JsonProperty("original_dependency")
        private String originalDependency;

        @JsonProperty("repaired_dependency")
        private String repairedDependency;

        @JsonProperty("repair_strategy")
        private String repairStrategy;

        @JsonProperty("confidence")
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Rollback conflict")
    public static class RollbackConflict {
        @JsonProperty("conflict_id")
        private String conflictId;

        @JsonProperty("type")
        private String type;

        @JsonProperty("component_id")
        private String componentId;

        @JsonProperty("description")
        private String description;

        @JsonProperty("current_value")
        private Object currentValue;

        @JsonProperty("rollback_value")
        private Object rollbackValue;

        @JsonProperty("severity")
        private String severity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Resolved conflict")
    public static class ResolvedConflict {
        @JsonProperty("conflict_id")
        private String conflictId;

        @JsonProperty("resolution_strategy")
        private String resolutionStrategy;

        @JsonProperty("resolved_value")
        private Object resolvedValue;

        @JsonProperty("confidence")
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Manual conflict requiring user input")
    public static class ManualConflict {
        @JsonProperty("conflict_id")
        private String conflictId;

        @JsonProperty("description")
        private String description;

        @JsonProperty("options")
        private List<ConflictOption> options;

        @JsonProperty("recommended_option")
        private String recommendedOption;

        @JsonProperty("impact")
        private String impact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Conflict option")
    public static class ConflictOption {
        @JsonProperty("option_id")
        private String optionId;

        @JsonProperty("label")
        private String label;

        @JsonProperty("description")
        private String description;

        @JsonProperty("value")
        private Object value;

        @JsonProperty("preview_url")
        private String previewUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Integrity verification result")
    public static class IntegrityVerification {
        @JsonProperty("verified")
        private Boolean verified;

        @JsonProperty("merkle_root_match")
        private Boolean merkleRootMatch;

        @JsonProperty("chain_intact")
        private Boolean chainIntact;

        @JsonProperty("checksum_match")
        private Boolean checksumMatch;

        @JsonProperty("verification_details")
        private Map<String, Object> verificationDetails;

        @JsonProperty("issues_found")
        private List<String> issuesFound;

        @JsonProperty("recommendations")
        private List<String> recommendations;
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Creates a successful rollback response
     */
    public static RollbackResponse success(String targetSnapshotId, Integer targetVersion,
                                           PageLayout restoredLayout, String newVersionVector,
                                           ChangeSummary changeSummary, long durationMs,
                                           String userId, String reason) {
        return RollbackResponse.builder()
                .success(true)
                .rollbackId("rollback_" + System.currentTimeMillis())
                .message(String.format("Successfully rolled back to version %d", targetVersion))
                .status("COMPLETED")
                .targetSnapshotId(targetSnapshotId)
                .targetVersion(targetVersion)
                .restoredLayout(restoredLayout)
                .newVersionVector(newVersionVector)
                .changeSummary(changeSummary)
                .durationMs(durationMs)
                .rolledBackAt(Instant.now())
                .rolledBackBy(userId)
                .reason(reason)
                .build();
    }

    /**
     * Creates a preview rollback response
     */
    public static RollbackResponse preview(String targetSnapshotId, Integer targetVersion,
                                           PageLayout previewLayout, ChangeSummary changeSummary,
                                           String userId, String reason) {
        String previewId = "preview_" + System.currentTimeMillis();

        return RollbackResponse.builder()
                .success(true)
                .rollbackId(previewId)
                .message("Preview of rollback to version " + targetVersion)
                .status("PREVIEW")
                .isPreview(true)
                .previewId(previewId)
                .previewExpiresAt(Instant.now().plusSeconds(3600))
                .targetSnapshotId(targetSnapshotId)
                .targetVersion(targetVersion)
                .restoredLayout(previewLayout)
                .changeSummary(changeSummary)
                .rolledBackBy(userId)
                .reason(reason)
                .rolledBackAt(Instant.now())
                .build();
    }

    /**
     * Creates a partial rollback response (some components preserved)
     */
    public static RollbackResponse partial(String targetSnapshotId, Integer targetVersion,
                                           PageLayout restoredLayout, List<String> preservedComponents,
                                           List<BrokenDependency> brokenDependencies,
                                           ChangeSummary changeSummary, long durationMs,
                                           String userId, String reason) {
        return RollbackResponse.builder()
                .success(true)
                .rollbackId("rollback_" + System.currentTimeMillis())
                .message("Partial rollback completed. Some components were preserved.")
                .status("PARTIAL")
                .targetSnapshotId(targetSnapshotId)
                .targetVersion(targetVersion)
                .restoredLayout(restoredLayout)
                .preservedComponents(preservedComponents)
                .brokenDependencies(brokenDependencies)
                .changeSummary(changeSummary)
                .durationMs(durationMs)
                .rolledBackAt(Instant.now())
                .rolledBackBy(userId)
                .reason(reason)
                .build();
    }

    /**
     * Creates a rollback response with conflicts
     */
    public static RollbackResponse withConflicts(String targetSnapshotId, Integer targetVersion,
                                                 List<RollbackConflict> conflicts,
                                                 List<ManualConflict> manualConflicts,
                                                 PageLayout currentLayout, String userId) {
        return RollbackResponse.builder()
                .success(false)
                .message("Rollback blocked by conflicts. Manual resolution required.")
                .status("CONFLICT")
                .targetSnapshotId(targetSnapshotId)
                .targetVersion(targetVersion)
                .restoredLayout(currentLayout)
                .conflicts(conflicts)
                .manualConflicts(manualConflicts)
                .rolledBackBy(userId)
                .rolledBackAt(Instant.now())
                .build();
    }

    /**
     * Creates a failed rollback response
     */
    public static RollbackResponse failed(String targetSnapshotId, Integer targetVersion,
                                          String errorMessage, String userId) {
        return RollbackResponse.builder()
                .success(false)
                .message("Rollback failed: " + errorMessage)
                .status("FAILED")
                .targetSnapshotId(targetSnapshotId)
                .targetVersion(targetVersion)
                .rolledBackBy(userId)
                .rolledBackAt(Instant.now())
                .build();
    }

    /**
     * Creates a rollback response with verification results
     */
    public static RollbackResponse withVerification(RollbackResponse response,
                                                    IntegrityVerification verification,
                                                    LayoutValidationResponse validation,
                                                    double verificationScore) {
        response.setIntegrityVerification(verification);
        response.setValidation(validation);
        response.setVerificationScore(verificationScore);
        return response;
    }

    /**
     * Checks if rollback can be undone
     */
    public boolean isUndoable() {
        return success && !isPreview && status.equals("COMPLETED");
    }

    /**
     * Gets rollback summary string
     */
    public String getRollbackSummary() {
        if (!success) {
            return "Rollback failed: " + message;
        }

        if (isPreview) {
            return String.format("Preview: Would roll back to version %d. %d components affected.",
                    targetVersion, changeSummary != null ? changeSummary.getTotalChanges() : 0);
        }

        return String.format("Rolled back to version %d. %d components restored, %d preserved.",
                targetVersion,
                changeSummary != null ? changeSummary.getComponentsRestored() : 0,
                preservedComponents != null ? preservedComponents.size() : 0);
    }

    /**
     * Gets verification status
     */
    public String getVerificationStatus() {
        if (integrityVerification == null) return "NOT_VERIFIED";
        if (integrityVerification.getVerified()) return "VERIFIED";
        return "VERIFICATION_FAILED";
    }

    /**
     * Gets overall rollback grade (A-F)
     */
    public String getRollbackGrade() {
        if (!success) return "F";

        if (verificationScore != null) {
            if (verificationScore >= 95) return "A+";
            if (verificationScore >= 90) return "A";
            if (verificationScore >= 80) return "B";
            if (verificationScore >= 70) return "C";
            if (verificationScore >= 60) return "D";
            return "F";
        }

        if (changeSummary == null) return "C";

        int totalChanges = changeSummary.getTotalChanges() != null ? changeSummary.getTotalChanges() : 0;
        if (totalChanges == 0) return "A";
        if (totalChanges < 10) return "B";
        if (totalChanges < 50) return "C";
        return "D";
    }

    /**
     * Checks if there are broken dependencies
     */
    public boolean hasBrokenDependencies() {
        return brokenDependencies != null && !brokenDependencies.isEmpty();
    }

    /**
     * Checks if there are orphaned components
     */
    public boolean hasOrphanedComponents() {
        return orphanedComponents != null && !orphanedComponents.isEmpty();
    }
}