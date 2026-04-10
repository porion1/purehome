package com.purehome.uicore.exception;

import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * FAANG-ULTRA CONFLICT EXCEPTION
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Intelligent Conflict Resolution Exception (ICRE)
 * - Provides rich conflict metadata for automatic resolution
 * - Includes vector clock information for causality tracking
 * - Supplies resolution strategies with confidence scores
 * - Embeds alternative versions for conflict recovery
 * - Supports distributed conflict propagation
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Getter
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // CONFLICT IDENTIFIERS
    // =========================================================================

    private final String conflictId;
    private final String conflictType;
    private final String resourceId;
    private final String resourceType;

    // =========================================================================
    // VERSION INFORMATION
    // =========================================================================

    private final String clientVersionVector;
    private final String serverVersionVector;
    private final String resolvedVersionVector;

    // =========================================================================
    // CONFLICT DETAILS
    // =========================================================================

    private final Map<String, Object> clientState;
    private final Map<String, Object> serverState;
    private final Map<String, Object> commonAncestor;
    private final List<ConflictDetail> conflictingChanges;

    // =========================================================================
    // RESOLUTION OPTIONS
    // =========================================================================

    private final List<ResolutionOption> resolutionOptions;
    private final Map<String, Object> autoMergePreview;

    // =========================================================================
    // METADATA
    // =========================================================================

    private final Instant detectedAt;
    private final String detectedBy;
    private final String correlationId;
    private final int retryCount;
    private final boolean autoResolvable;
    private final double autoResolveConfidence;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    public ConflictException(String message) {
        super(message);
        this.conflictId = UUID.randomUUID().toString();
        this.conflictType = "UNKNOWN";
        this.resourceId = null;
        this.resourceType = null;
        this.clientVersionVector = null;
        this.serverVersionVector = null;
        this.resolvedVersionVector = null;
        this.clientState = null;
        this.serverState = null;
        this.commonAncestor = null;
        this.conflictingChanges = null;
        this.resolutionOptions = null;
        this.autoMergePreview = null;
        this.detectedAt = Instant.now();
        this.detectedBy = "SYSTEM";
        this.correlationId = null;
        this.retryCount = 0;
        this.autoResolvable = false;
        this.autoResolveConfidence = 0.0;
    }

    public ConflictException(String message, String conflictType) {
        this(message, conflictType, null, null);
    }

    public ConflictException(String message, String conflictType, String resourceId, String resourceType) {
        super(message);
        this.conflictId = UUID.randomUUID().toString();
        this.conflictType = conflictType;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.clientVersionVector = null;
        this.serverVersionVector = null;
        this.resolvedVersionVector = null;
        this.clientState = null;
        this.serverState = null;
        this.commonAncestor = null;
        this.conflictingChanges = null;
        this.resolutionOptions = null;
        this.autoMergePreview = null;
        this.detectedAt = Instant.now();
        this.detectedBy = "SYSTEM";
        this.correlationId = null;
        this.retryCount = 0;
        this.autoResolvable = false;
        this.autoResolveConfidence = 0.0;
    }

    public ConflictException(String message, String conflictType, Map<String, Object> clientState,
                             Map<String, Object> serverState, List<ResolutionOption> resolutionOptions) {
        this(message, conflictType, clientState, serverState, resolutionOptions, null);
    }

    public ConflictException(String message, String conflictType, Map<String, Object> clientState,
                             Map<String, Object> serverState, List<ResolutionOption> resolutionOptions,
                             Map<String, Object> autoMergePreview) {
        super(message);
        this.conflictId = UUID.randomUUID().toString();
        this.conflictType = conflictType;
        this.resourceId = null;
        this.resourceType = null;
        this.clientVersionVector = null;
        this.serverVersionVector = null;
        this.resolvedVersionVector = null;
        this.clientState = clientState;
        this.serverState = serverState;
        this.commonAncestor = null;
        this.conflictingChanges = null;
        this.resolutionOptions = resolutionOptions;
        this.autoMergePreview = autoMergePreview;
        this.detectedAt = Instant.now();
        this.detectedBy = "SYSTEM";
        this.correlationId = null;
        this.retryCount = 0;
        this.autoResolvable = resolutionOptions != null &&
                resolutionOptions.stream().anyMatch(ResolutionOption::isAutoApplicable);
        this.autoResolveConfidence = calculateAutoResolveConfidence(resolutionOptions);
    }

    public ConflictException(String message, String conflictType, String clientVersionVector,
                             String serverVersionVector, List<ConflictDetail> conflictingChanges,
                             List<ResolutionOption> resolutionOptions) {
        super(message);
        this.conflictId = UUID.randomUUID().toString();
        this.conflictType = conflictType;
        this.resourceId = null;
        this.resourceType = null;
        this.clientVersionVector = clientVersionVector;
        this.serverVersionVector = serverVersionVector;
        this.resolvedVersionVector = null;
        this.clientState = null;
        this.serverState = null;
        this.commonAncestor = null;
        this.conflictingChanges = conflictingChanges;
        this.resolutionOptions = resolutionOptions;
        this.autoMergePreview = null;
        this.detectedAt = Instant.now();
        this.detectedBy = "SYSTEM";
        this.correlationId = null;
        this.retryCount = 0;
        this.autoResolvable = resolutionOptions != null &&
                resolutionOptions.stream().anyMatch(ResolutionOption::isAutoApplicable);
        this.autoResolveConfidence = calculateAutoResolveConfidence(resolutionOptions);
    }

    // =========================================================================
    // BUILDER PATTERN
    // =========================================================================

    public static class Builder {
        private String message;
        private String conflictType = "CONCURRENT_MODIFICATION";
        private String resourceId;
        private String resourceType;
        private String clientVersionVector;
        private String serverVersionVector;
        private String resolvedVersionVector;
        private Map<String, Object> clientState;
        private Map<String, Object> serverState;
        private Map<String, Object> commonAncestor;
        private List<ConflictDetail> conflictingChanges;
        private List<ResolutionOption> resolutionOptions;
        private Map<String, Object> autoMergePreview;
        private String detectedBy = "SYSTEM";
        private String correlationId;
        private int retryCount = 0;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder conflictType(String conflictType) {
            this.conflictType = conflictType;
            return this;
        }

        public Builder resource(String resourceId, String resourceType) {
            this.resourceId = resourceId;
            this.resourceType = resourceType;
            return this;
        }

        public Builder versionVectors(String client, String server) {
            this.clientVersionVector = client;
            this.serverVersionVector = server;
            return this;
        }

        public Builder resolvedVersionVector(String resolved) {
            this.resolvedVersionVector = resolved;
            return this;
        }

        public Builder states(Map<String, Object> client, Map<String, Object> server) {
            this.clientState = client;
            this.serverState = server;
            return this;
        }

        public Builder commonAncestor(Map<String, Object> ancestor) {
            this.commonAncestor = ancestor;
            return this;
        }

        public Builder conflictingChanges(List<ConflictDetail> changes) {
            this.conflictingChanges = changes;
            return this;
        }

        public Builder resolutionOptions(List<ResolutionOption> options) {
            this.resolutionOptions = options;
            return this;
        }

        public Builder autoMergePreview(Map<String, Object> preview) {
            this.autoMergePreview = preview;
            return this;
        }

        public Builder detectedBy(String detectedBy) {
            this.detectedBy = detectedBy;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public ConflictException build() {
            return new ConflictException(this);
        }
    }

    private ConflictException(Builder builder) {
        super(builder.message);
        this.conflictId = UUID.randomUUID().toString();
        this.conflictType = builder.conflictType;
        this.resourceId = builder.resourceId;
        this.resourceType = builder.resourceType;
        this.clientVersionVector = builder.clientVersionVector;
        this.serverVersionVector = builder.serverVersionVector;
        this.resolvedVersionVector = builder.resolvedVersionVector;
        this.clientState = builder.clientState;
        this.serverState = builder.serverState;
        this.commonAncestor = builder.commonAncestor;
        this.conflictingChanges = builder.conflictingChanges;
        this.resolutionOptions = builder.resolutionOptions;
        this.autoMergePreview = builder.autoMergePreview;
        this.detectedAt = Instant.now();
        this.detectedBy = builder.detectedBy;
        this.correlationId = builder.correlationId;
        this.retryCount = builder.retryCount;
        this.autoResolvable = builder.resolutionOptions != null &&
                builder.resolutionOptions.stream().anyMatch(ResolutionOption::isAutoApplicable);
        this.autoResolveConfidence = calculateAutoResolveConfidence(builder.resolutionOptions);
    }

    public static Builder builder() {
        return new Builder();
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Getter
    public static class ConflictDetail {
        private final String field;
        private final Object clientValue;
        private final Object serverValue;
        private final Object ancestorValue;
        private final String changeType;
        private final String resolutionStrategy;

        public ConflictDetail(String field, Object clientValue, Object serverValue,
                              Object ancestorValue, String changeType) {
            this.field = field;
            this.clientValue = clientValue;
            this.serverValue = serverValue;
            this.ancestorValue = ancestorValue;
            this.changeType = changeType;
            this.resolutionStrategy = null;
        }

        public ConflictDetail(String field, Object clientValue, Object serverValue,
                              Object ancestorValue, String changeType, String resolutionStrategy) {
            this.field = field;
            this.clientValue = clientValue;
            this.serverValue = serverValue;
            this.ancestorValue = ancestorValue;
            this.changeType = changeType;
            this.resolutionStrategy = resolutionStrategy;
        }

        public boolean isSameField() {
            return clientValue != null && serverValue != null &&
                    clientValue.equals(serverValue);
        }

        public String getSummary() {
            return String.format("Field '%s': client=%s, server=%s, ancestor=%s",
                    field, clientValue, serverValue, ancestorValue);
        }
    }

    @Getter
    public static class ResolutionOption {
        private final String id;
        private final String strategy;
        private final String description;
        private final Map<String, Object> result;
        private final double confidence;
        private final boolean autoApplicable;

        public ResolutionOption(String id, String strategy, String description,
                                Map<String, Object> result, double confidence) {
            this(id, strategy, description, result, confidence, false);
        }

        public ResolutionOption(String id, String strategy, String description,
                                Map<String, Object> result, double confidence, boolean autoApplicable) {
            this.id = id;
            this.strategy = strategy;
            this.description = description;
            this.result = result;
            this.confidence = confidence;
            this.autoApplicable = autoApplicable;
        }

        public String getSummary() {
            return String.format("%s (%.1f%%): %s", strategy, confidence * 100, description);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private static double calculateAutoResolveConfidence(List<ResolutionOption> options) {
        if (options == null || options.isEmpty()) return 0.0;

        return options.stream()
                .filter(ResolutionOption::isAutoApplicable)
                .mapToDouble(ResolutionOption::getConfidence)
                .max()
                .orElse(0.0);
    }

    /**
     * Check if conflict can be automatically resolved
     */
    public boolean canAutoResolve() {
        return autoResolvable && autoResolveConfidence >= 0.8;
    }

    /**
     * Get the best auto-resolution option
     */
    public ResolutionOption getBestAutoResolution() {
        if (resolutionOptions == null) return null;

        return resolutionOptions.stream()
                .filter(ResolutionOption::isAutoApplicable)
                .max((a, b) -> Double.compare(a.getConfidence(), b.getConfidence()))
                .orElse(null);
    }

    /**
     * Get conflict severity (0-100)
     */
    public int getSeverity() {
        if (conflictingChanges == null || conflictingChanges.isEmpty()) {
            return 50;
        }

        int severity = 0;
        for (ConflictDetail detail : conflictingChanges) {
            if ("CRITICAL".equals(detail.getChangeType())) {
                severity += 30;
            } else if ("HIGH".equals(detail.getChangeType())) {
                severity += 20;
            } else if ("MEDIUM".equals(detail.getChangeType())) {
                severity += 10;
            } else {
                severity += 5;
            }
        }

        return Math.min(100, severity);
    }

    /**
     * Get conflict summary
     */
    public String getConflictSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Conflict ID: ").append(conflictId).append("\n");
        sb.append("Type: ").append(conflictType).append("\n");
        sb.append("Message: ").append(getMessage()).append("\n");

        if (conflictingChanges != null && !conflictingChanges.isEmpty()) {
            sb.append("Conflicting Changes: ").append(conflictingChanges.size()).append("\n");
            for (ConflictDetail detail : conflictingChanges) {
                sb.append("  - ").append(detail.getSummary()).append("\n");
            }
        }

        if (resolutionOptions != null && !resolutionOptions.isEmpty()) {
            sb.append("Resolution Options:\n");
            for (ResolutionOption option : resolutionOptions) {
                sb.append("  - ").append(option.getSummary()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Get compact representation for logging
     */
    public String getCompactLog() {
        return String.format("Conflict[%s] %s: %s (autoResolvable=%s, confidence=%.2f)",
                conflictType, conflictId, getMessage(), autoResolvable, autoResolveConfidence);
    }
}