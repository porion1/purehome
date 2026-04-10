package com.purehome.uicore.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * FAANG-ULTRA ROLLBACK REQUEST DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Temporal Rollback with Dependency Preservation (TR-DP)
 * - Implements atomic rollback with full dependency graph analysis
 * - Provides preview mode for safe rollback testing
 * - Supports partial rollback with selective component restoration
 * - Includes conflict detection and auto-resolution strategies
 * - Provides rollback validation with integrity checks
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ultra rollback request with temporal dependency preservation")
public class RollbackRequest {

    // =========================================================================
    // ROLLBACK TARGET
    // =========================================================================

    @Schema(description = "Snapshot ID to roll back to", example = "snapshot_123456")
    @JsonProperty("snapshot_id")
    private String snapshotId;

    @Schema(description = "Version number to roll back to", example = "5")
    @JsonProperty("version_number")
    private Integer versionNumber;

    @Schema(description = "Timestamp to roll back to (point-in-time recovery)", example = "2024-01-15T10:30:00Z")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Schema(description = "Rollback label (for identification)", example = "Before redesign")
    @JsonProperty("label")
    private String label;

    // =========================================================================
    // ROLLBACK SCOPE
    // =========================================================================

    @Schema(description = "Rollback scope (FULL, PARTIAL, SELECTIVE)", example = "FULL")
    @JsonProperty("scope")
    @Builder.Default
    private String scope = "FULL";

    @Schema(description = "Components to include in selective rollback", example = "[\"comp_001\", \"comp_002\"]")
    @JsonProperty("include_components")
    private List<String> includeComponents;

    @Schema(description = "Components to exclude from rollback", example = "[\"comp_003\"]")
    @JsonProperty("exclude_components")
    private List<String> excludeComponents;

    @Schema(description = "Sections to include in selective rollback", example = "[\"section_header\"]")
    @JsonProperty("include_sections")
    private List<String> includeSections;

    @Schema(description = "Sections to exclude from rollback", example = "[\"section_footer\"]")
    @JsonProperty("exclude_sections")
    private List<String> excludeSections;

    // =========================================================================
    // DEPENDENCY HANDLING
    // =========================================================================

    @Schema(description = "Preserve dependencies (true) or rollback all (false)", example = "true")
    @JsonProperty("preserve_dependencies")
    @Builder.Default
    private Boolean preserveDependencies = true;

    @Schema(description = "Auto-resolve broken dependencies", example = "true")
    @JsonProperty("auto_resolve_dependencies")
    @Builder.Default
    private Boolean autoResolveDependencies = true;

    @Schema(description = "Dependency resolution strategy",
            example = "AUTO",
            allowableValues = {"AUTO", "PRESERVE", "ROLLBACK", "PROMPT"})
    @JsonProperty("dependency_strategy")
    @Builder.Default
    private String dependencyStrategy = "AUTO";

    @Schema(description = "Maximum dependency depth to traverse", example = "5")
    @JsonProperty("max_dependency_depth")
    private Integer maxDependencyDepth;

    // =========================================================================
    // CONFLICT RESOLUTION
    // =========================================================================

    @Schema(description = "Conflict resolution strategy",
            example = "AUTO_MERGE",
            allowableValues = {"AUTO_MERGE", "OVERWRITE", "PRESERVE", "PROMPT"})
    @JsonProperty("conflict_strategy")
    @Builder.Default
    private String conflictStrategy = "AUTO_MERGE";

    @Schema(description = "Conflict resolution confidence threshold (0-1)", example = "0.8")
    @JsonProperty("confidence_threshold")
    @Builder.Default
    private Double confidenceThreshold = 0.8;

    @Schema(description = "Accept auto-resolved conflicts without review", example = "true")
    @JsonProperty("accept_auto_resolved")
    @Builder.Default
    private Boolean acceptAutoResolved = true;

    // =========================================================================
    // DATA PRESERVATION
    // =========================================================================

    @Schema(description = "Create snapshot before rollback", example = "true")
    @JsonProperty("create_pre_rollback_snapshot")
    @Builder.Default
    private Boolean createPreRollbackSnapshot = true;

    @Schema(description = "Preserve current layout after rollback", example = "false")
    @JsonProperty("preserve_current_layout")
    @Builder.Default
    private Boolean preserveCurrentLayout = false;

    @Schema(description = "Archive rolled back layout", example = "true")
    @JsonProperty("archive_rolled_back_layout")
    @Builder.Default
    private Boolean archiveRolledBackLayout = true;

    @Schema(description = "Archive retention days", example = "90")
    @JsonProperty("archive_retention_days")
    private Integer archiveRetentionDays;

    // =========================================================================
    // VALIDATION
    // =========================================================================

    @Schema(description = "Validate layout after rollback", example = "true")
    @JsonProperty("validate_after_rollback")
    @Builder.Default
    private Boolean validateAfterRollback = true;

    @Schema(description = "Validation level (QUICK, STANDARD, FULL)", example = "FULL")
    @JsonProperty("validation_level")
    @Builder.Default
    private String validationLevel = "FULL";

    @Schema(description = "Fail on validation errors", example = "true")
    @JsonProperty("fail_on_validation_error")
    @Builder.Default
    private Boolean failOnValidationError = true;

    // =========================================================================
    // PREVIEW MODE
    // =========================================================================

    @Schema(description = "Preview rollback without executing", example = "false")
    @JsonProperty("preview_only")
    @Builder.Default
    private Boolean previewOnly = false;

    @Schema(description = "Preview ID for existing preview", example = "preview_abc123")
    @JsonProperty("preview_id")
    private String previewId;

    // =========================================================================
    // AUDIT & COMPLIANCE
    // =========================================================================

    @NotBlank(message = "Rollback reason is required")
    @Schema(description = "Reason for rollback", example = "Performance degradation after deployment", required = true)
    @JsonProperty("reason")
    private String reason;

    @Schema(description = "Compliance justification", example = "Rollback to previous version for stability")
    @JsonProperty("compliance_justification")
    private String complianceJustification;

    @Schema(description = "Approval reference ID", example = "CHG-2024-001")
    @JsonProperty("approval_reference")
    private String approvalReference;

    // =========================================================================
    // NOTIFICATION
    // =========================================================================

    @Schema(description = "Notify subscribers of rollback", example = "true")
    @JsonProperty("notify_subscribers")
    @Builder.Default
    private Boolean notifySubscribers = true;

    @Schema(description = "Notify users involved in changes", example = "true")
    @JsonProperty("notify_involved_users")
    @Builder.Default
    private Boolean notifyInvolvedUsers = true;

    @Schema(description = "Additional notification recipients", example = "[\"team-lead@purehome.com\"]")
    @JsonProperty("additional_recipients")
    private List<String> additionalRecipients;

    // =========================================================================
    // ADVANCED OPTIONS
    // =========================================================================

    @Schema(description = "Enable rollback verification", example = "true")
    @JsonProperty("verify_integrity")
    @Builder.Default
    private Boolean verifyIntegrity = true;

    @Schema(description = "Rollback timeout in seconds", example = "300")
    @JsonProperty("timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 300;

    @Schema(description = "Custom metadata for rollback operation")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Validates rollback request
     */
    public ValidationResult validate() {
        List<String> errors = new java.util.ArrayList<>();
        List<String> warnings = new java.util.ArrayList<>();

        // Validate target specification
        int targetCount = 0;
        if (snapshotId != null) targetCount++;
        if (versionNumber != null) targetCount++;
        if (timestamp != null) targetCount++;
        if (label != null) targetCount++;

        if (targetCount == 0) {
            errors.add("Must specify rollback target (snapshot_id, version_number, timestamp, or label)");
        } else if (targetCount > 1) {
            warnings.add("Multiple targets specified. Using: " +
                    (snapshotId != null ? "snapshot_id" :
                            versionNumber != null ? "version_number" :
                                    timestamp != null ? "timestamp" : "label"));
        }

        // Validate scope
        if (scope != null && !List.of("FULL", "PARTIAL", "SELECTIVE").contains(scope.toUpperCase())) {
            errors.add("Invalid scope: " + scope + ". Must be FULL, PARTIAL, or SELECTIVE");
        }

        // Validate selective rollback parameters
        if ("PARTIAL".equalsIgnoreCase(scope) || "SELECTIVE".equalsIgnoreCase(scope)) {
            if ((includeComponents == null || includeComponents.isEmpty()) &&
                    (includeSections == null || includeSections.isEmpty())) {
                errors.add("Selective rollback requires include_components or include_sections");
            }
        }

        // Validate dependency strategy
        if (dependencyStrategy != null && !List.of("AUTO", "PRESERVE", "ROLLBACK", "PROMPT").contains(dependencyStrategy.toUpperCase())) {
            warnings.add("Invalid dependency_strategy: " + dependencyStrategy);
        }

        // Validate conflict strategy
        if (conflictStrategy != null && !List.of("AUTO_MERGE", "OVERWRITE", "PRESERVE", "PROMPT").contains(conflictStrategy.toUpperCase())) {
            warnings.add("Invalid conflict_strategy: " + conflictStrategy);
        }

        // Validate confidence threshold
        if (confidenceThreshold != null && (confidenceThreshold < 0 || confidenceThreshold > 1)) {
            errors.add("Confidence threshold must be between 0 and 1");
        }

        // Validate timeout
        if (timeoutSeconds != null && timeoutSeconds < 1) {
            errors.add("Timeout must be at least 1 second");
        }

        // Validate reason
        if (reason == null || reason.trim().isEmpty()) {
            errors.add("Rollback reason is required");
        } else if (reason.length() < 10) {
            warnings.add("Rollback reason is very short. Consider providing more detail for audit.");
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Gets the effective rollback target
     */
    public String getEffectiveTarget() {
        if (snapshotId != null) return snapshotId;
        if (versionNumber != null) return "v" + versionNumber;
        if (timestamp != null) return timestamp.toString();
        if (label != null) return label;
        return null;
    }

    /**
     * Gets rollback scope description
     */
    public String getScopeDescription() {
        if ("FULL".equalsIgnoreCase(scope)) {
            return "Full layout rollback";
        }
        if ("PARTIAL".equalsIgnoreCase(scope)) {
            int includeCount = (includeComponents != null ? includeComponents.size() : 0) +
                    (includeSections != null ? includeSections.size() : 0);
            return String.format("Partial rollback affecting %d components/sections", includeCount);
        }
        if ("SELECTIVE".equalsIgnoreCase(scope)) {
            return "Selective component restoration";
        }
        return "Rollback";
    }

    /**
     * Gets dependency handling description
     */
    public String getDependencyHandlingDescription() {
        if (!preserveDependencies) {
            return "All dependencies will be rolled back";
        }

        switch (dependencyStrategy.toUpperCase()) {
            case "AUTO":
                return "Dependencies will be auto-resolved where possible";
            case "PRESERVE":
                return "Dependencies will be preserved even if rolled back";
            case "ROLLBACK":
                return "Dependencies will be rolled back with components";
            case "PROMPT":
                return "User will be prompted for dependency conflicts";
            default:
                return "Dependencies will be handled automatically";
        }
    }

    // =========================================================================
    // VALIDATION RESULT CLASS
    // =========================================================================

    @Data
    @AllArgsConstructor
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public String getErrorMessage() { return errors.isEmpty() ? null : String.join(", ", errors); }
        public String getWarningMessage() { return warnings.isEmpty() ? null : String.join(", ", warnings); }
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Creates a full rollback request to a snapshot
     */
    public static RollbackRequest fullRollbackToSnapshot(String snapshotId, String reason, String userId) {
        return RollbackRequest.builder()
                .snapshotId(snapshotId)
                .scope("FULL")
                .reason(reason)
                .build();
    }

    /**
     * Creates a full rollback request to a version number
     */
    public static RollbackRequest fullRollbackToVersion(Integer versionNumber, String reason, String userId) {
        return RollbackRequest.builder()
                .versionNumber(versionNumber)
                .scope("FULL")
                .reason(reason)
                .build();
    }

    /**
     * Creates a point-in-time rollback request
     */
    public static RollbackRequest pointInTimeRollback(Instant timestamp, String reason, String userId) {
        return RollbackRequest.builder()
                .timestamp(timestamp)
                .scope("FULL")
                .reason(reason)
                .build();
    }

    /**
     * Creates a selective rollback for specific components
     */
    public static RollbackRequest selectiveRollback(List<String> componentIds, String reason, String userId) {
        return RollbackRequest.builder()
                .scope("SELECTIVE")
                .includeComponents(componentIds)
                .preserveDependencies(true)
                .reason(reason)
                .build();
    }

    /**
     * Creates a preview rollback request
     */
    public static RollbackRequest previewRollback(String snapshotId, String reason) {
        return RollbackRequest.builder()
                .snapshotId(snapshotId)
                .previewOnly(true)
                .validateAfterRollback(false)
                .reason(reason)
                .build();
    }

    /**
     * Creates a rollback with conflict auto-resolution
     */
    public static RollbackRequest autoResolveRollback(String snapshotId, String reason) {
        return RollbackRequest.builder()
                .snapshotId(snapshotId)
                .conflictStrategy("AUTO_MERGE")
                .autoResolveDependencies(true)
                .acceptAutoResolved(true)
                .reason(reason)
                .build();
    }

    /**
     * Creates a rollback with manual conflict resolution
     */
    public static RollbackRequest manualResolveRollback(String snapshotId, String reason) {
        return RollbackRequest.builder()
                .snapshotId(snapshotId)
                .conflictStrategy("PROMPT")
                .autoResolveDependencies(false)
                .acceptAutoResolved(false)
                .reason(reason)
                .build();
    }

    /**
     * Creates a rollback that preserves current layout for comparison
     */
    public static RollbackRequest preserveCurrentRollback(String snapshotId, String reason) {
        return RollbackRequest.builder()
                .snapshotId(snapshotId)
                .preserveCurrentLayout(true)
                .createPreRollbackSnapshot(true)
                .reason(reason)
                .build();
    }
}