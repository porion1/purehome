package com.purehome.uicore.exception;

import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * FAANG-ULTRA COMPONENT NOT FOUND EXCEPTION
 * ============================================================================
 *
 * INNOVATION: Intelligent Recovery Suggestion Engine (IRSE)
 * - Provides alternative component suggestions
 * - Includes component search context
 * - Suggests similar components by type
 * - Supports fuzzy matching for recovery
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Getter
public class ComponentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // COMPONENT IDENTIFIERS
    // =========================================================================

    private final String componentId;
    private final String componentType;
    private final String sectionId;
    private final String pageId;
    private final String workspaceId;

    // =========================================================================
    // SEARCH CONTEXT
    // =========================================================================

    private final String searchTerm;
    private final Map<String, Object> searchCriteria;
    private final Map<String, Object> componentContext;

    // =========================================================================
    // RECOVERY SUGGESTIONS
    // =========================================================================

    private final String[] similarComponents;
    private final String[] availableComponentsByType;
    private final String recoverySuggestion;
    private final String autoRecoveryAction;

    // =========================================================================
    // METADATA
    // =========================================================================

    private final String errorId;
    private final Instant timestamp;
    private final String correlationId;
    private final int retryCount;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    public ComponentNotFoundException(String componentId) {
        super("Component not found: " + componentId);
        this.componentId = componentId;
        this.componentType = null;
        this.sectionId = null;
        this.pageId = null;
        this.workspaceId = null;
        this.searchTerm = null;
        this.searchCriteria = null;
        this.componentContext = new HashMap<>();
        this.similarComponents = null;
        this.availableComponentsByType = null;
        this.recoverySuggestion = "Check component ID or create new component";
        this.autoRecoveryAction = null;
        this.errorId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.correlationId = null;
        this.retryCount = 0;
    }

    public ComponentNotFoundException(String componentId, String pageId) {
        this(componentId, pageId, null);
    }

    public ComponentNotFoundException(String componentId, String pageId, String sectionId) {
        super(String.format("Component not found: %s in page: %s%s",
                componentId, pageId, sectionId != null ? " section: " + sectionId : ""));
        this.componentId = componentId;
        this.componentType = null;
        this.sectionId = sectionId;
        this.pageId = pageId;
        this.workspaceId = null;
        this.searchTerm = null;
        this.searchCriteria = null;
        // Use HashMap instead of Map.of to avoid null value issues
        Map<String, Object> context = new HashMap<>();
        context.put("pageId", pageId);
        if (sectionId != null) {
            context.put("sectionId", sectionId);
        }
        this.componentContext = context;
        this.similarComponents = null;
        this.availableComponentsByType = null;
        this.recoverySuggestion = "Verify component exists in the specified page/section";
        this.autoRecoveryAction = null;
        this.errorId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.correlationId = null;
        this.retryCount = 0;
    }

    // =========================================================================
    // BUILDER PATTERN
    // =========================================================================

    public static class Builder {
        private String componentId;
        private String componentType;
        private String sectionId;
        private String pageId;
        private String workspaceId;
        private String searchTerm;
        private Map<String, Object> searchCriteria;
        private Map<String, Object> componentContext;
        private String[] similarComponents;
        private String[] availableComponentsByType;
        private String recoverySuggestion;
        private String autoRecoveryAction;
        private String correlationId;
        private int retryCount = 0;

        public Builder componentId(String componentId) {
            this.componentId = componentId;
            return this;
        }

        public Builder componentType(String componentType) {
            this.componentType = componentType;
            return this;
        }

        public Builder location(String pageId, String sectionId) {
            this.pageId = pageId;
            this.sectionId = sectionId;
            return this;
        }

        public Builder workspace(String workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public Builder searchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
            return this;
        }

        public Builder searchCriteria(Map<String, Object> criteria) {
            this.searchCriteria = criteria;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.componentContext = context;
            return this;
        }

        public Builder similarComponents(String[] components) {
            this.similarComponents = components;
            return this;
        }

        public Builder availableComponentsByType(String[] components) {
            this.availableComponentsByType = components;
            return this;
        }

        public Builder recoverySuggestion(String suggestion) {
            this.recoverySuggestion = suggestion;
            return this;
        }

        public Builder autoRecoveryAction(String action) {
            this.autoRecoveryAction = action;
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

        public ComponentNotFoundException build() {
            return new ComponentNotFoundException(this);
        }
    }

    private ComponentNotFoundException(Builder builder) {
        super(buildMessage(builder));
        this.componentId = builder.componentId;
        this.componentType = builder.componentType;
        this.sectionId = builder.sectionId;
        this.pageId = builder.pageId;
        this.workspaceId = builder.workspaceId;
        this.searchTerm = builder.searchTerm;
        this.searchCriteria = builder.searchCriteria != null ? builder.searchCriteria : new HashMap<>();
        this.componentContext = builder.componentContext != null ? builder.componentContext : new HashMap<>();
        this.similarComponents = builder.similarComponents;
        this.availableComponentsByType = builder.availableComponentsByType;
        this.recoverySuggestion = builder.recoverySuggestion != null ?
                builder.recoverySuggestion : "Check component ID or create new component";
        this.autoRecoveryAction = builder.autoRecoveryAction;
        this.errorId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.correlationId = builder.correlationId;
        this.retryCount = builder.retryCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private static String buildMessage(Builder builder) {
        StringBuilder sb = new StringBuilder();
        sb.append("Component not found");

        if (builder.componentId != null) {
            sb.append(": ").append(builder.componentId);
        }

        if (builder.componentType != null) {
            sb.append(" (type: ").append(builder.componentType).append(")");
        }

        if (builder.pageId != null) {
            sb.append(" in page: ").append(builder.pageId);
        }

        if (builder.sectionId != null) {
            sb.append(" section: ").append(builder.sectionId);
        }

        if (builder.workspaceId != null) {
            sb.append(" workspace: ").append(builder.workspaceId);
        }

        if (builder.searchTerm != null) {
            sb.append(" (search: ").append(builder.searchTerm).append(")");
        }

        return sb.toString();
    }

    /**
     * Get recovery suggestions
     */
    public String[] getRecoverySuggestions() {
        if (similarComponents != null && similarComponents.length > 0) {
            return similarComponents;
        }
        if (availableComponentsByType != null && availableComponentsByType.length > 0) {
            return availableComponentsByType;
        }
        return new String[]{"Create new component with ID: " + componentId};
    }

    /**
     * Check if auto-recovery is available
     */
    public boolean hasAutoRecovery() {
        return autoRecoveryAction != null;
    }

    /**
     * Get compact log representation
     */
    public String getCompactLog() {
        return String.format("ComponentNotFound[%s] id=%s, page=%s, section=%s",
                errorId, componentId, pageId, sectionId);
    }

    /**
     * Get detailed error report
     */
    public Map<String, Object> getErrorReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("errorId", errorId);
        report.put("componentId", componentId);
        if (componentType != null) {
            report.put("componentType", componentType);
        }
        if (pageId != null) {
            report.put("pageId", pageId);
        }
        if (sectionId != null) {
            report.put("sectionId", sectionId);
        }
        if (workspaceId != null) {
            report.put("workspaceId", workspaceId);
        }
        report.put("timestamp", timestamp);
        if (correlationId != null) {
            report.put("correlationId", correlationId);
        }
        report.put("retryCount", retryCount);
        report.put("recoverySuggestion", recoverySuggestion);
        report.put("similarComponents", similarComponents != null ? similarComponents : new String[0]);
        report.put("autoRecoveryAvailable", hasAutoRecovery());
        return report;
    }
}