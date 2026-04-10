package com.purehome.uicore.exception;

import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * FAANG-ULTRA LAYOUT VALIDATION EXCEPTION
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Predictive Validation Exception (PVE)
 * - Provides comprehensive validation error details
 * - Includes auto-fix suggestions with confidence scoring
 * - Supports nested validation errors with hierarchy
 * - Provides validation context for debugging
 * - Includes severity scoring for prioritization
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Getter
public class LayoutValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // VALIDATION IDENTIFIERS
    // =========================================================================

    private final String validationId;
    private final String pageId;
    private final String workspaceId;
    private final String layoutVersion;

    // =========================================================================
    // VALIDATION RESULTS
    // =========================================================================

    private final boolean valid;
    private final double complianceScore;
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;
    private final List<ValidationInfo> info;

    // =========================================================================
    // AUTO-FIX SUGGESTIONS
    // =========================================================================

    private final List<AutoFixSuggestion> autoFixSuggestions;
    private final Map<String, Object> autoFixedLayout;
    private final double autoFixConfidence;

    // =========================================================================
    // CONSTRAINT VIOLATIONS
    // =========================================================================

    private final List<ConstraintViolation> constraintViolations;
    private final List<GridViolation> gridViolations;
    private final List<DependencyViolation> dependencyViolations;

    // =========================================================================
    // METADATA
    // =========================================================================

    private final Instant validatedAt;
    private final long validationTimeMs;
    private final String validationLevel;
    private final String correlationId;

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Getter
    public static class ValidationError {
        private final String code;
        private final String message;
        private final String field;
        private final String componentId;
        private final String sectionId;
        private final String suggestedFix;
        private final int severity; // 0-100
        private final boolean autoFixable;

        public ValidationError(String code, String message, String field,
                               String componentId, String sectionId) {
            this(code, message, field, componentId, sectionId, null, 50, false);
        }

        public ValidationError(String code, String message, String field,
                               String componentId, String sectionId,
                               String suggestedFix, int severity, boolean autoFixable) {
            this.code = code;
            this.message = message;
            this.field = field;
            this.componentId = componentId;
            this.sectionId = sectionId;
            this.suggestedFix = suggestedFix;
            this.severity = severity;
            this.autoFixable = autoFixable;
        }

        public String getSummary() {
            return String.format("[%s] %s: %s", code, field != null ? field : "layout", message);
        }
    }

    @Getter
    public static class ValidationWarning {
        private final String code;
        private final String message;
        private final String field;
        private final String componentId;
        private final String recommendation;
        private final int impact;

        public ValidationWarning(String code, String message, String field,
                                 String componentId, String recommendation, int impact) {
            this.code = code;
            this.message = message;
            this.field = field;
            this.componentId = componentId;
            this.recommendation = recommendation;
            this.impact = impact;
        }

        public String getSummary() {
            return String.format("[WARN] %s: %s", code, message);
        }
    }

    @Getter
    public static class ValidationInfo {
        private final String code;
        private final String message;
        private final String suggestion;

        public ValidationInfo(String code, String message, String suggestion) {
            this.code = code;
            this.message = message;
            this.suggestion = suggestion;
        }
    }

    @Getter
    public static class AutoFixSuggestion {
        private final String id;
        private final String action;
        private final String description;
        private final Map<String, Object> parameters;
        private final double confidence;
        private final boolean destructive;

        public AutoFixSuggestion(String id, String action, String description,
                                 Map<String, Object> parameters, double confidence, boolean destructive) {
            this.id = id;
            this.action = action;
            this.description = description;
            this.parameters = parameters;
            this.confidence = confidence;
            this.destructive = destructive;
        }

        public String getSummary() {
            return String.format("%s (%.1f%%): %s", action, confidence * 100, description);
        }
    }

    @Getter
    public static class ConstraintViolation {
        private final String constraintId;
        private final String constraintName;
        private final String targetId;
        private final String targetType;
        private final Object currentValue;
        private final Object expectedValue;
        private final String remediation;

        public ConstraintViolation(String constraintId, String constraintName,
                                   String targetId, String targetType,
                                   Object currentValue, Object expectedValue,
                                   String remediation) {
            this.constraintId = constraintId;
            this.constraintName = constraintName;
            this.targetId = targetId;
            this.targetType = targetType;
            this.currentValue = currentValue;
            this.expectedValue = expectedValue;
            this.remediation = remediation;
        }
    }

    @Getter
    public static class GridViolation {
        private final String componentId;
        private final int currentX;
        private final int currentY;
        private final int currentWidth;
        private final int currentHeight;
        private final int suggestedX;
        private final int suggestedY;
        private final String violationType;

        public GridViolation(String componentId, int currentX, int currentY,
                             int currentWidth, int currentHeight,
                             int suggestedX, int suggestedY, String violationType) {
            this.componentId = componentId;
            this.currentX = currentX;
            this.currentY = currentY;
            this.currentWidth = currentWidth;
            this.currentHeight = currentHeight;
            this.suggestedX = suggestedX;
            this.suggestedY = suggestedY;
            this.violationType = violationType;
        }
    }

    @Getter
    public static class DependencyViolation {
        private final String componentId;
        private final String dependsOn;
        private final String dependencyType;
        private final boolean circular;
        private final List<String> cyclePath;
        private final String resolution;

        public DependencyViolation(String componentId, String dependsOn,
                                   String dependencyType, boolean circular,
                                   List<String> cyclePath, String resolution) {
            this.componentId = componentId;
            this.dependsOn = dependsOn;
            this.dependencyType = dependencyType;
            this.circular = circular;
            this.cyclePath = cyclePath != null ? cyclePath : new ArrayList<>();
            this.resolution = resolution;
        }
    }

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    public LayoutValidationException(String message) {
        super(message);
        this.validationId = UUID.randomUUID().toString();
        this.pageId = null;
        this.workspaceId = null;
        this.layoutVersion = null;
        this.valid = false;
        this.complianceScore = 0.0;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.info = new ArrayList<>();
        this.autoFixSuggestions = new ArrayList<>();
        this.autoFixedLayout = null;
        this.autoFixConfidence = 0.0;
        this.constraintViolations = new ArrayList<>();
        this.gridViolations = new ArrayList<>();
        this.dependencyViolations = new ArrayList<>();
        this.validatedAt = Instant.now();
        this.validationTimeMs = 0;
        this.validationLevel = "QUICK";
        this.correlationId = null;
    }

    public LayoutValidationException(String message, List<ValidationError> errors) {
        this(message, errors, null, null);
    }

    public LayoutValidationException(String message, List<ValidationError> errors,
                                     List<ValidationWarning> warnings) {
        this(message, errors, warnings, null);
    }

    public LayoutValidationException(String message, List<ValidationError> errors,
                                     List<ValidationWarning> warnings, List<ValidationInfo> info) {
        super(message);
        this.validationId = UUID.randomUUID().toString();
        this.pageId = null;
        this.workspaceId = null;
        this.layoutVersion = null;
        this.valid = errors == null || errors.isEmpty();
        this.complianceScore = calculateComplianceScore(errors, warnings);
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.info = info != null ? info : new ArrayList<>();
        this.autoFixSuggestions = generateAutoFixSuggestions(this.errors);
        this.autoFixedLayout = null;
        this.autoFixConfidence = calculateAutoFixConfidence(this.autoFixSuggestions);
        this.constraintViolations = new ArrayList<>();
        this.gridViolations = new ArrayList<>();
        this.dependencyViolations = new ArrayList<>();
        this.validatedAt = Instant.now();
        this.validationTimeMs = 0;
        this.validationLevel = "STANDARD";
        this.correlationId = null;
    }

    // =========================================================================
    // BUILDER PATTERN
    // =========================================================================

    public static class Builder {
        private String message;
        private String pageId;
        private String workspaceId;
        private String layoutVersion;
        private List<ValidationError> errors = new ArrayList<>();
        private List<ValidationWarning> warnings = new ArrayList<>();
        private List<ValidationInfo> info = new ArrayList<>();
        private List<AutoFixSuggestion> autoFixSuggestions = new ArrayList<>();
        private Map<String, Object> autoFixedLayout;
        private List<ConstraintViolation> constraintViolations = new ArrayList<>();
        private List<GridViolation> gridViolations = new ArrayList<>();
        private List<DependencyViolation> dependencyViolations = new ArrayList<>();
        private long validationTimeMs;
        private String validationLevel = "STANDARD";
        private String correlationId;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder pageId(String pageId) {
            this.pageId = pageId;
            return this;
        }

        public Builder workspaceId(String workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public Builder layoutVersion(String layoutVersion) {
            this.layoutVersion = layoutVersion;
            return this;
        }

        public Builder addError(ValidationError error) {
            this.errors.add(error);
            return this;
        }

        public Builder addErrors(List<ValidationError> errors) {
            this.errors.addAll(errors);
            return this;
        }

        public Builder addWarning(ValidationWarning warning) {
            this.warnings.add(warning);
            return this;
        }

        public Builder addWarnings(List<ValidationWarning> warnings) {
            this.warnings.addAll(warnings);
            return this;
        }

        public Builder addInfo(ValidationInfo info) {
            this.info.add(info);
            return this;
        }

        public Builder addAutoFixSuggestion(AutoFixSuggestion suggestion) {
            this.autoFixSuggestions.add(suggestion);
            return this;
        }

        public Builder autoFixedLayout(Map<String, Object> layout) {
            this.autoFixedLayout = layout;
            return this;
        }

        public Builder addConstraintViolation(ConstraintViolation violation) {
            this.constraintViolations.add(violation);
            return this;
        }

        public Builder addGridViolation(GridViolation violation) {
            this.gridViolations.add(violation);
            return this;
        }

        public Builder addDependencyViolation(DependencyViolation violation) {
            this.dependencyViolations.add(violation);
            return this;
        }

        public Builder validationTimeMs(long timeMs) {
            this.validationTimeMs = timeMs;
            return this;
        }

        public Builder validationLevel(String level) {
            this.validationLevel = level;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public LayoutValidationException build() {
            return new LayoutValidationException(this);
        }
    }

    private LayoutValidationException(Builder builder) {
        super(builder.message);
        this.validationId = UUID.randomUUID().toString();
        this.pageId = builder.pageId;
        this.workspaceId = builder.workspaceId;
        this.layoutVersion = builder.layoutVersion;
        this.valid = builder.errors.isEmpty();
        this.complianceScore = calculateComplianceScore(builder.errors, builder.warnings);
        this.errors = builder.errors;
        this.warnings = builder.warnings;
        this.info = builder.info;
        this.autoFixSuggestions = !builder.autoFixSuggestions.isEmpty() ?
                builder.autoFixSuggestions : generateAutoFixSuggestions(builder.errors);
        this.autoFixedLayout = builder.autoFixedLayout;
        this.autoFixConfidence = calculateAutoFixConfidence(this.autoFixSuggestions);
        this.constraintViolations = builder.constraintViolations;
        this.gridViolations = builder.gridViolations;
        this.dependencyViolations = builder.dependencyViolations;
        this.validatedAt = Instant.now();
        this.validationTimeMs = builder.validationTimeMs;
        this.validationLevel = builder.validationLevel;
        this.correlationId = builder.correlationId;
    }

    public static Builder builder() {
        return new Builder();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private static double calculateComplianceScore(List<ValidationError> errors,
                                                   List<ValidationWarning> warnings) {
        if ((errors == null || errors.isEmpty()) && (warnings == null || warnings.isEmpty())) {
            return 100.0;
        }

        double errorPenalty = 0;
        if (errors != null) {
            for (ValidationError error : errors) {
                errorPenalty += error.getSeverity() / 100.0;
            }
        }

        double warningPenalty = 0;
        if (warnings != null) {
            warningPenalty = warnings.size() * 0.05;
        }

        double totalPenalty = Math.min(1.0, errorPenalty + warningPenalty);
        return Math.max(0, 100.0 * (1 - totalPenalty));
    }

    private static List<AutoFixSuggestion> generateAutoFixSuggestions(List<ValidationError> errors) {
        List<AutoFixSuggestion> suggestions = new ArrayList<>();

        if (errors == null) return suggestions;

        for (ValidationError error : errors) {
            if (error.isAutoFixable() && error.getSuggestedFix() != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("field", error.getField());
                params.put("componentId", error.getComponentId());
                params.put("suggestedValue", error.getSuggestedFix());

                suggestions.add(new AutoFixSuggestion(
                        UUID.randomUUID().toString(),
                        "AUTO_FIX_" + error.getCode(),
                        error.getSuggestedFix(),
                        params,
                        0.85,
                        false
                ));
            }
        }

        return suggestions;
    }

    private static double calculateAutoFixConfidence(List<AutoFixSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) return 0.0;

        return suggestions.stream()
                .mapToDouble(AutoFixSuggestion::getConfidence)
                .average()
                .orElse(0.0);
    }

    /**
     * Get critical errors (severity >= 70)
     */
    public List<ValidationError> getCriticalErrors() {
        return errors.stream()
                .filter(e -> e.getSeverity() >= 70)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get auto-fixable errors
     */
    public List<ValidationError> getAutoFixableErrors() {
        return errors.stream()
                .filter(ValidationError::isAutoFixable)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Check if exception has auto-fix suggestions
     */
    public boolean hasAutoFixSuggestions() {
        return autoFixSuggestions != null && !autoFixSuggestions.isEmpty();
    }

    /**
     * Get highest severity error
     */
    public ValidationError getMostSevereError() {
        return errors.stream()
                .max((a, b) -> Integer.compare(a.getSeverity(), b.getSeverity()))
                .orElse(null);
    }

    /**
     * Get validation summary
     */
    public String getValidationSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Layout Validation Failed\n");
        sb.append("========================\n");
        sb.append("Validation ID: ").append(validationId).append("\n");
        sb.append("Page ID: ").append(pageId != null ? pageId : "N/A").append("\n");
        sb.append("Compliance Score: ").append(String.format("%.1f%%", complianceScore)).append("\n");
        sb.append("Errors: ").append(errors.size()).append("\n");
        sb.append("Warnings: ").append(warnings.size()).append("\n");
        sb.append("Auto-Fix Available: ").append(hasAutoFixSuggestions()).append("\n");

        if (!errors.isEmpty()) {
            sb.append("\nErrors:\n");
            for (ValidationError error : errors) {
                sb.append("  - ").append(error.getSummary()).append("\n");
            }
        }

        if (hasAutoFixSuggestions()) {
            sb.append("\nAuto-Fix Suggestions:\n");
            for (AutoFixSuggestion suggestion : autoFixSuggestions) {
                sb.append("  - ").append(suggestion.getSummary()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Get detailed error report
     */
    public Map<String, Object> getErrorReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("validationId", validationId);
        report.put("pageId", pageId);
        report.put("workspaceId", workspaceId);
        report.put("layoutVersion", layoutVersion);
        report.put("valid", valid);
        report.put("complianceScore", complianceScore);
        report.put("errorCount", errors.size());
        report.put("warningCount", warnings.size());
        report.put("errors", errors);
        report.put("warnings", warnings);
        report.put("autoFixAvailable", hasAutoFixSuggestions());
        report.put("autoFixConfidence", autoFixConfidence);
        report.put("validationLevel", validationLevel);
        report.put("validatedAt", validatedAt);
        report.put("validationTimeMs", validationTimeMs);
        report.put("correlationId", correlationId);
        return report;
    }

    /**
     * Get compact log representation
     */
    public String getCompactLog() {
        return String.format("LayoutValidationException[%s] page=%s, score=%.1f%%, errors=%d, autoFix=%s",
                validationId, pageId, complianceScore, errors.size(), hasAutoFixSuggestions());
    }
}