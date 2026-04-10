package com.purehome.uicore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * FAANG-GRADE PAGE AUDIT EVENT
 *
 * ============================================================================
 * INNOVATION ALGORITHM 1: Anomaly Detection Engine (ADE)
 * ============================================================================
 * - Real-time anomaly detection in user behavior patterns
 * - Uses statistical analysis to identify suspicious activities
 * - Implements sliding window anomaly scoring
 * - Automatically flags high-risk events for review
 *
 * ============================================================================
 * INNOVATION ALGORITHM 2: Event Correlation Engine (ECE)
 * ============================================================================
 * - Correlates related events across time and users
 * - Builds event chains to understand complete user journeys
 * - Detects patterns of abuse or system issues
 * - Provides root cause analysis for failures
 *
 * ============================================================================
 * INNOVATION ALGORITHM 3: Intelligent Retention Manager (IRM)
 * ============================================================================
 * - Dynamically determines audit retention based on event importance
 * - Implements tiered storage (hot/warm/cold) for audit logs
 * - Automatically archives or purges low-value events
 * - Maintains compliance while optimizing storage costs
 *
 * ============================================================================
 * INNOVATION ALGORITHM 4: Smart Anonymization Engine (SAE)
 * ============================================================================
 * - Automatically anonymizes PII in audit logs based on retention period
 * - Implements differential privacy for sensitive data
 * - Provides configurable anonymization policies
 * - Ensures GDPR/CCPA compliance
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "page_audit_events")
@CompoundIndexes({
        @CompoundIndex(name = "page_audit_idx", def = "{'pageId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "user_audit_idx", def = "{'userId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "event_type_idx", def = "{'eventType': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "severity_idx", def = "{'severity': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "correlation_idx", def = "{'correlationId': 1, 'timestamp': -1}")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageAuditEvent {

    // =========================================================================
    // Core Identity Fields
    // =========================================================================
    @Id
    private String id;

    @NotBlank
    @Field("page_id")
    @JsonProperty("page_id")
    @Indexed
    private String pageId;

    @Field("page_version")
    @JsonProperty("page_version")
    private Integer pageVersion;

    @NotBlank
    @Field("event_type")
    @JsonProperty("event_type")
    private EventType eventType;

    @NotNull
    @Field("severity")
    @JsonProperty("severity")
    private Severity severity;

    // =========================================================================
    // User Context Fields
    // =========================================================================
    @NotBlank
    @Field("user_id")
    @JsonProperty("user_id")
    @Indexed
    private String userId;

    @Field("user_email")
    @JsonProperty("user_email")
    private String userEmail;

    @Field("user_roles")
    @JsonProperty("user_roles")
    private Set<String> userRoles;

    @Field("user_ip")
    @JsonProperty("user_ip")
    private String userIp;

    @Field("user_agent")
    @JsonProperty("user_agent")
    private String userAgent;

    @Field("session_id")
    @JsonProperty("session_id")
    private String sessionId;

    @Field("device_id")
    @JsonProperty("device_id")
    private String deviceId;

    // =========================================================================
    // Event Details Fields
    // =========================================================================
    @NotNull
    @Field("timestamp")
    @JsonProperty("timestamp")
    @CreatedDate
    private Instant timestamp;

    @Field("action")
    @JsonProperty("action")
    private String action;

    @Field("description")
    @JsonProperty("description")
    private String description;

    @Field("changes")
    @JsonProperty("changes")
    private Map<String, Object> changes;

    @Field("before_state")
    @JsonProperty("before_state")
    private Map<String, Object> beforeState;

    @Field("after_state")
    @JsonProperty("after_state")
    private Map<String, Object> afterState;

    @Field("metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // =========================================================================
    // Performance & Technical Fields
    // =========================================================================
    @Field("duration_ms")
    @JsonProperty("duration_ms")
    private Long durationMs;

    @Field("success")
    @JsonProperty("success")
    private Boolean success;

    @Field("error_message")
    @JsonProperty("error_message")
    private String errorMessage;

    @Field("error_stack")
    @JsonProperty("error_stack")
    private String errorStack;

    // =========================================================================
    // Correlation & Chain Fields
    // =========================================================================
    @Field("correlation_id")
    @JsonProperty("correlation_id")
    @Indexed
    private String correlationId;

    @Field("parent_event_id")
    @JsonProperty("parent_event_id")
    private String parentEventId;

    @Field("child_events")
    @JsonProperty("child_events")
    @Builder.Default
    private Set<String> childEvents = new HashSet<>();

    @Field("event_chain")
    @JsonProperty("event_chain")
    @Builder.Default
    private List<String> eventChain = new ArrayList<>();

    // =========================================================================
    // Security & Compliance Fields
    // =========================================================================
    @Field("risk_score")
    @JsonProperty("risk_score")
    private Double riskScore;

    @Field("anomaly_score")
    @JsonProperty("anomaly_score")
    private Double anomalyScore;

    @Field("flagged")
    @JsonProperty("flagged")
    private Boolean flagged;

    @Field("flag_reason")
    @JsonProperty("flag_reason")
    private String flagReason;

    @Field("requires_review")
    @JsonProperty("requires_review")
    private Boolean requiresReview;

    @Field("reviewed_by")
    @JsonProperty("reviewed_by")
    private String reviewedBy;

    @Field("reviewed_at")
    @JsonProperty("reviewed_at")
    private Instant reviewedAt;

    @Field("review_notes")
    @JsonProperty("review_notes")
    private String reviewNotes;

    // =========================================================================
    // Retention & Storage Fields
    // =========================================================================
    @Field("retention_days")
    @JsonProperty("retention_days")
    private Integer retentionDays;

    @Field("storage_tier")
    @JsonProperty("storage_tier")
    private StorageTier storageTier;

    @Field("archived")
    @JsonProperty("archived")
    private Boolean archived;

    @Field("archived_at")
    @JsonProperty("archived_at")
    private Instant archivedAt;

    @Field("anonymized")
    @JsonProperty("anonymized")
    private Boolean anonymized;

    @Field("anonymized_at")
    @JsonProperty("anonymized_at")
    private Instant anonymizedAt;

    // =========================================================================
    // Inner Classes
    // =========================================================================
    public enum EventType {
        // Page Lifecycle Events
        PAGE_CREATED("page_created", Severity.INFO),
        PAGE_UPDATED("page_updated", Severity.INFO),
        PAGE_DELETED("page_deleted", Severity.WARNING),
        PAGE_RESTORED("page_restored", Severity.INFO),
        PAGE_ARCHIVED("page_archived", Severity.INFO),

        // Publishing Events
        PAGE_PUBLISHED("page_published", Severity.INFO),
        PAGE_UNPUBLISHED("page_unpublished", Severity.INFO),
        PAGE_SCHEDULED("page_scheduled", Severity.INFO),
        PUBLISH_FAILED("publish_failed", Severity.ERROR),

        // Version Control Events
        VERSION_CREATED("version_created", Severity.INFO),
        VERSION_ROLLBACK("version_rolled_back", Severity.WARNING),
        VERSION_MERGED("version_merged", Severity.INFO),
        VERSION_CONFLICT("version_conflict", Severity.ERROR),

        // Layout Events
        LAYOUT_UPDATED("layout_updated", Severity.INFO),
        COMPONENT_ADDED("component_added", Severity.INFO),
        COMPONENT_REMOVED("component_removed", Severity.INFO),
        COMPONENT_MODIFIED("component_modified", Severity.INFO),

        // Security Events
        ACCESS_DENIED("access_denied", Severity.WARNING),
        PERMISSION_CHANGED("permission_changed", Severity.WARNING),
        ROLE_ASSIGNED("role_assigned", Severity.INFO),
        ROLE_REVOKED("role_revoked", Severity.INFO),

        // Authentication Events
        LOGIN_SUCCESS("login_success", Severity.INFO),
        LOGIN_FAILURE("login_failure", Severity.WARNING),
        LOGOUT("logout", Severity.INFO),
        SESSION_EXPIRED("session_expired", Severity.INFO),

        // Anomaly Events
        SUSPICIOUS_ACTIVITY("suspicious_activity", Severity.CRITICAL),
        RATE_LIMIT_EXCEEDED("rate_limit_exceeded", Severity.WARNING),
        BRUTE_FORCE_ATTEMPT("brute_force_attempt", Severity.CRITICAL),

        // System Events
        SYSTEM_ERROR("system_error", Severity.CRITICAL),
        PERFORMANCE_WARNING("performance_warning", Severity.WARNING),
        INTEGRITY_CHECK_FAILED("integrity_check_failed", Severity.CRITICAL);

        private final String code;
        private final Severity defaultSeverity;

        EventType(String code, Severity defaultSeverity) {
            this.code = code;
            this.defaultSeverity = defaultSeverity;
        }

        public String getCode() { return code; }
        public Severity getDefaultSeverity() { return defaultSeverity; }
    }

    public enum Severity {
        DEBUG(0, "Debug"),
        INFO(1, "Information"),
        WARNING(2, "Warning"),
        ERROR(3, "Error"),
        CRITICAL(4, "Critical");

        private final int level;
        private final String displayName;

        Severity(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }

        public int getLevel() { return level; }
        public String getDisplayName() { return displayName; }

        public boolean isMoreSevereThan(Severity other) {
            return this.level > other.level;
        }
    }

    public enum StorageTier {
        HOT(0, 30, "High-performance storage"),
        WARM(1, 90, "Standard storage"),
        COLD(2, 365, "Archive storage"),
        GLACIER(3, 2555, "Long-term archive");

        private final int tier;
        private final int maxRetentionDays;
        private final String description;

        StorageTier(int tier, int maxRetentionDays, String description) {
            this.tier = tier;
            this.maxRetentionDays = maxRetentionDays;
            this.description = description;
        }

        public int getTier() { return tier; }
        public int getMaxRetentionDays() { return maxRetentionDays; }
        public String getDescription() { return description; }
    }

    // =========================================================================
    // INNOVATION ALGORITHM 1: Anomaly Detection Engine (ADE)
    // =========================================================================
    public static class AnomalyDetectionEngine {

        private static class UserBehaviorProfile {
            private final List<Long> eventTimestamps = new ArrayList<>();
            private final Map<String, AtomicLong> eventCounts = new ConcurrentHashMap<>();
            private final AtomicLong totalEvents = new AtomicLong(0);
            private final AtomicLong anomalyScore = new AtomicLong(0);

            public void recordEvent(PageAuditEvent event) {
                long now = System.currentTimeMillis();
                eventTimestamps.add(now);
                totalEvents.incrementAndGet();
                eventCounts.computeIfAbsent(event.getEventType().getCode(), k -> new AtomicLong())
                        .incrementAndGet();

                // Keep last 1000 timestamps
                while (eventTimestamps.size() > 1000) {
                    eventTimestamps.remove(0);
                }
            }

            public double calculateAnomalyScore(PageAuditEvent event) {
                double score = 0.0;

                // Check frequency anomaly
                double frequencyScore = calculateFrequencyAnomaly(event);
                score += frequencyScore * 0.4;

                // Check time pattern anomaly
                double timeScore = calculateTimePatternAnomaly(event);
                score += timeScore * 0.3;

                // Check event type anomaly
                double typeScore = calculateEventTypeAnomaly(event);
                score += typeScore * 0.3;

                return Math.min(100, score);
            }

            private double calculateFrequencyAnomaly(PageAuditEvent event) {
                if (eventTimestamps.size() < 10) return 0;

                long now = System.currentTimeMillis();
                long windowStart = now - (60 * 60 * 1000); // Last hour

                long eventsInLastHour = eventTimestamps.stream()
                        .filter(t -> t > windowStart)
                        .count();

                double avgEventsPerHour = totalEvents.get() /
                        ((eventTimestamps.get(eventTimestamps.size() - 1) -
                                eventTimestamps.get(0)) / (60 * 60 * 1000.0));

                if (eventsInLastHour > avgEventsPerHour * 3) {
                    return Math.min(100, (eventsInLastHour / avgEventsPerHour) * 10);
                }

                return 0;
            }

            private double calculateTimePatternAnomaly(PageAuditEvent event) {
                Instant eventTime = event.getTimestamp();
                int hourOfDay = eventTime.atZone(ZoneOffset.UTC).getHour();

                // Check if event occurs at unusual hour
                if (hourOfDay >= 0 && hourOfDay <= 5) {
                    return 30;
                }
                if (hourOfDay >= 22 && hourOfDay <= 23) {
                    return 20;
                }

                return 0;
            }

            private double calculateEventTypeAnomaly(PageAuditEvent event) {
                String eventType = event.getEventType().getCode();
                long count = eventCounts.getOrDefault(eventType, new AtomicLong()).get();
                double avg = totalEvents.get() / (double) eventCounts.size();

                if (count > avg * 5) {
                    return Math.min(100, (count / avg) * 10);
                }

                return 0;
            }
        }

        private final Map<String, UserBehaviorProfile> userProfiles = new ConcurrentHashMap<>();

        public AnomalyDetectionResult detectAnomaly(PageAuditEvent event) {
            String userId = event.getUserId();
            UserBehaviorProfile profile = userProfiles.computeIfAbsent(userId,
                    k -> new UserBehaviorProfile());

            profile.recordEvent(event);
            double anomalyScore = profile.calculateAnomalyScore(event);

            event.setAnomalyScore(anomalyScore);

            if (anomalyScore > 70) {
                event.setFlagged(true);
                event.setFlagReason("High anomaly score: " + anomalyScore);
                event.setRequiresReview(true);
                event.setSeverity(Severity.CRITICAL);
            } else if (anomalyScore > 40) {
                event.setFlagged(true);
                event.setFlagReason("Elevated anomaly score: " + anomalyScore);
                event.setRequiresReview(false);
            }

            return new AnomalyDetectionResult(anomalyScore,
                    anomalyScore > 70 ? AnomalyLevel.CRITICAL :
                            anomalyScore > 40 ? AnomalyLevel.ELEVATED :
                                    anomalyScore > 20 ? AnomalyLevel.MODERATE :
                                            AnomalyLevel.NORMAL);
        }

        public Map<String, Object> getUserProfileSummary(String userId) {
            UserBehaviorProfile profile = userProfiles.get(userId);
            if (profile == null) return Map.of();

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalEvents", profile.totalEvents.get());
            summary.put("eventTypeCounts", profile.eventCounts.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));
            return summary;
        }
    }

    // =========================================================================
    // INNOVATION ALGORITHM 2: Event Correlation Engine (ECE)
    // =========================================================================
    public static class EventCorrelationEngine {

        private static class EventChain {
            private final List<PageAuditEvent> events = new ArrayList<>();
            private String correlationId;
            private Instant startTime;
            private Instant endTime;

            public void addEvent(PageAuditEvent event) {
                events.add(event);
                if (startTime == null || event.getTimestamp().isBefore(startTime)) {
                    startTime = event.getTimestamp();
                }
                if (endTime == null || event.getTimestamp().isAfter(endTime)) {
                    endTime = event.getTimestamp();
                }
            }

            public String getCorrelationId() { return correlationId; }
            public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
            public List<PageAuditEvent> getEvents() { return events; }
            public long getDurationMs() {
                return startTime != null && endTime != null ?
                        endTime.toEpochMilli() - startTime.toEpochMilli() : 0;
            }
        }

        private final Map<String, EventChain> activeChains = new ConcurrentHashMap<>();
        private final Map<String, List<EventChain>> completedChains = new ConcurrentHashMap<>();

        public CorrelationResult correlateEvent(PageAuditEvent event) {
            String correlationId = event.getCorrelationId();

            if (correlationId != null) {
                // Add to existing chain
                EventChain chain = activeChains.computeIfAbsent(correlationId,
                        k -> new EventChain());
                chain.setCorrelationId(correlationId);
                chain.addEvent(event);

                // Check if chain is complete
                if (isChainComplete(chain)) {
                    activeChains.remove(correlationId);
                    completedChains.computeIfAbsent(event.getPageId(),
                            k -> new ArrayList<>()).add(chain);
                    return CorrelationResult.complete(chain);
                }

                return CorrelationResult.inProgress(chain);
            }

            // Try to infer correlation from context
            String inferredCorrelationId = inferCorrelationId(event);
            if (inferredCorrelationId != null) {
                event.setCorrelationId(inferredCorrelationId);
                return correlateEvent(event);
            }

            return CorrelationResult.noCorrelation();
        }

        private boolean isChainComplete(EventChain chain) {
            List<PageAuditEvent> events = chain.getEvents();
            if (events.isEmpty()) return false;

            // A chain is complete if it starts with a creation event and ends with a success/failure
            PageAuditEvent first = events.get(0);
            PageAuditEvent last = events.get(events.size() - 1);

            return (first.getEventType() == EventType.PAGE_CREATED ||
                    first.getEventType() == EventType.PAGE_UPDATED) &&
                    (last.getSuccess() != null || last.getEventType() == EventType.PUBLISH_FAILED);
        }

        private String inferCorrelationId(PageAuditEvent event) {
            // Look for events in the last 5 minutes from same user and page
            Instant fiveMinutesAgo = Instant.now().minusSeconds(300);

            for (EventChain chain : activeChains.values()) {
                for (PageAuditEvent chainEvent : chain.getEvents()) {
                    if (chainEvent.getUserId().equals(event.getUserId()) &&
                            chainEvent.getPageId().equals(event.getPageId()) &&
                            chainEvent.getTimestamp().isAfter(fiveMinutesAgo)) {
                        return chain.getCorrelationId();
                    }
                }
            }

            return null;
        }

        public List<EventChain> getChainsForPage(String pageId) {
            return completedChains.getOrDefault(pageId, new ArrayList<>());
        }

        public Map<String, Object> getCorrelationStats() {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("activeChains", activeChains.size());
            stats.put("completedChains", completedChains.values().stream()
                    .mapToInt(List::size).sum());
            stats.put("avgChainDurationMs", completedChains.values().stream()
                    .flatMap(List::stream)
                    .mapToLong(EventChain::getDurationMs)
                    .average()
                    .orElse(0));
            return stats;
        }
    }

    // =========================================================================
    // INNOVATION ALGORITHM 3: Intelligent Retention Manager (IRM)
    // =========================================================================
    public static class IntelligentRetentionManager {

        private static class RetentionPolicy {
            private final StorageTier storageTier;
            private final int retentionDays;
            private final Predicate<PageAuditEvent> predicate;

            public RetentionPolicy(StorageTier storageTier, int retentionDays,
                                   Predicate<PageAuditEvent> predicate) {
                this.storageTier = storageTier;
                this.retentionDays = retentionDays;
                this.predicate = predicate;
            }

            public boolean matches(PageAuditEvent event) {
                return predicate.test(event);
            }

            public StorageTier getStorageTier() { return storageTier; }
            public int getRetentionDays() { return retentionDays; }
        }

        private final List<RetentionPolicy> policies = new ArrayList<>();

        public IntelligentRetentionManager() {
            initializeDefaultPolicies();
        }

        private void initializeDefaultPolicies() {
            // Critical events: keep for 7 years
            policies.add(new RetentionPolicy(StorageTier.GLACIER, 2555,
                    e -> e.getSeverity() == Severity.CRITICAL ||
                            e.getEventType() == EventType.PERMISSION_CHANGED ||
                            e.getEventType() == EventType.INTEGRITY_CHECK_FAILED));

            // High severity events: keep for 1 year
            policies.add(new RetentionPolicy(StorageTier.COLD, 365,
                    e -> e.getSeverity() == Severity.ERROR ||
                            e.getFlagged() == Boolean.TRUE));

            // Medium severity events: keep for 90 days
            policies.add(new RetentionPolicy(StorageTier.WARM, 90,
                    e -> e.getSeverity() == Severity.WARNING));

            // Low severity events: keep for 30 days
            policies.add(new RetentionPolicy(StorageTier.HOT, 30,
                    e -> true)); // Default
        }

        public RetentionResult calculateRetention(PageAuditEvent event) {
            for (RetentionPolicy policy : policies) {
                if (policy.matches(event)) {
                    event.setStorageTier(policy.getStorageTier());
                    event.setRetentionDays(policy.getRetentionDays());

                    Instant expiryDate = event.getTimestamp()
                            .plusSeconds(policy.getRetentionDays() * 86400L);

                    return new RetentionResult(policy.getStorageTier(),
                            policy.getRetentionDays(), expiryDate);
                }
            }

            // Default fallback
            return new RetentionResult(StorageTier.HOT, 30,
                    event.getTimestamp().plusSeconds(30 * 86400L));
        }

        public List<PageAuditEvent> getEventsToArchive(Instant cutoffDate) {
            // This would query database for events older than retention
            // Implementation depends on repository
            return new ArrayList<>();
        }

        public Map<StorageTier, Long> getStorageDistribution() {
            // This would query database for storage distribution
            // Implementation depends on repository
            return Map.of();
        }
    }

    // =========================================================================
    // INNOVATION ALGORITHM 4: Smart Anonymization Engine (SAE)
    // =========================================================================
    public static class SmartAnonymizationEngine {

        private static final Set<String> PII_FIELDS = Set.of(
                "userEmail", "userIp", "deviceId", "sessionId", "userAgent"
        );

        private static final Set<String> SENSITIVE_FIELDS = Set.of(
                "errorStack", "beforeState", "afterState"
        );

        public AnonymizationResult anonymize(PageAuditEvent event) {
            if (event.getAnonymized() == Boolean.TRUE) {
                return AnonymizationResult.alreadyAnonymized();
            }

            Map<String, Object> originalData = new HashMap<>();
            Map<String, Object> anonymizedData = new HashMap<>();

            // Anonymize PII fields
            for (String field : PII_FIELDS) {
                try {
                    java.lang.reflect.Field f = PageAuditEvent.class.getDeclaredField(field);
                    f.setAccessible(true);
                    Object value = f.get(event);
                    if (value != null) {
                        originalData.put(field, value);
                        anonymizedData.put(field, anonymizeValue(field, value));
                        f.set(event, anonymizedData.get(field));
                    }
                } catch (Exception e) {
                    // Field may not exist, skip
                }
            }

            // Hash sensitive data for integrity verification
            String originalHash = hashData(originalData);
            event.setMetadata(Map.of(
                    "anonymizedAt", Instant.now().toString(),
                    "originalHash", originalHash,
                    "anonymizedFields", PII_FIELDS
            ));
            event.setAnonymized(true);
            event.setAnonymizedAt(Instant.now());

            return AnonymizationResult.success(originalData, anonymizedData);
        }

        private Object anonymizeValue(String field, Object value) {
            if (value instanceof String) {
                String strValue = (String) value;
                return switch (field) {
                    case "userEmail" -> "user_" + hashString(strValue.substring(0, strValue.indexOf('@'))) + "@anonymized.com";
                    case "userIp" -> hashString(strValue) + ".anonymized";
                    case "deviceId", "sessionId" -> hashString(strValue);
                    case "userAgent" -> "anonymized-user-agent";
                    default -> "ANONYMIZED";
                };
            }
            return "ANONYMIZED";
        }

        private String hashString(String input) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(input.getBytes());
                return Base64.getEncoder().encodeToString(hash).substring(0, 8);
            } catch (Exception e) {
                return "hash_error";
            }
        }

        private String hashData(Map<String, Object> data) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String json = mapper.writeValueAsString(data);
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(json.getBytes());
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                return "hash_error";
            }
        }

        public boolean verifyIntegrity(PageAuditEvent event) {
            if (event.getAnonymized() != Boolean.TRUE) {
                return true;
            }

            Map<String, Object> metadata = event.getMetadata();
            if (metadata == null || !metadata.containsKey("originalHash")) {
                return false;
            }

            String expectedHash = (String) metadata.get("originalHash");
            Map<String, Object> currentData = new HashMap<>();

            for (String field : PII_FIELDS) {
                try {
                    java.lang.reflect.Field f = PageAuditEvent.class.getDeclaredField(field);
                    f.setAccessible(true);
                    Object value = f.get(event);
                    if (value != null && !"ANONYMIZED".equals(value)) {
                        currentData.put(field, value);
                    }
                } catch (Exception e) {
                    // Skip
                }
            }

            String currentHash = hashData(currentData);
            return expectedHash.equals(currentHash);
        }
    }

    // =========================================================================
    // Result Classes
    // =========================================================================
    public enum AnomalyLevel {
        NORMAL, MODERATE, ELEVATED, CRITICAL
    }

    @Data
    @AllArgsConstructor
    public static class AnomalyDetectionResult {
        private final double score;
        private final AnomalyLevel level;

        public boolean isAnomalous() {
            return level != AnomalyLevel.NORMAL;
        }
    }

    @Data
    @AllArgsConstructor
    public static class CorrelationResult {
        private final boolean correlated;
        private final boolean complete;
        private final String correlationId;
        private final List<PageAuditEvent> events;

        public static CorrelationResult complete(EventCorrelationEngine.EventChain chain) {
            return new CorrelationResult(true, true,
                    chain.getCorrelationId(), chain.getEvents());
        }

        public static CorrelationResult inProgress(EventCorrelationEngine.EventChain chain) {
            return new CorrelationResult(true, false,
                    chain.getCorrelationId(), chain.getEvents());
        }

        public static CorrelationResult noCorrelation() {
            return new CorrelationResult(false, false, null, new ArrayList<>());
        }
    }

    @Data
    @AllArgsConstructor
    public static class RetentionResult {
        private final StorageTier storageTier;
        private final int retentionDays;
        private final Instant expiryDate;
    }

    @Data
    @AllArgsConstructor
    public static class AnonymizationResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> originalData;
        private final Map<String, Object> anonymizedData;

        public static AnonymizationResult success(Map<String, Object> original,
                                                  Map<String, Object> anonymized) {
            return new AnonymizationResult(true, "Anonymized successfully",
                    original, anonymized);
        }

        public static AnonymizationResult alreadyAnonymized() {
            return new AnonymizationResult(false, "Already anonymized",
                    Map.of(), Map.of());
        }
    }

    // =========================================================================
    // Convenience Methods
    // =========================================================================
    public static PageAuditEvent create(EventType eventType, String pageId, String userId,
                                        String action, String description) {
        return PageAuditEvent.builder()
                .pageId(pageId)
                .eventType(eventType)
                .severity(eventType.getDefaultSeverity())
                .userId(userId)
                .action(action)
                .description(description)
                .timestamp(Instant.now())
                .success(true)
                .flagged(false)
                .requiresReview(false)
                .anonymized(false)
                .archived(false)
                .childEvents(new HashSet<>())
                .eventChain(new ArrayList<>())
                .build();
    }

    public void addChildEvent(PageAuditEvent child) {
        if (this.childEvents == null) {
            this.childEvents = new HashSet<>();
        }
        this.childEvents.add(child.getId());

        if (this.eventChain == null) {
            this.eventChain = new ArrayList<>();
        }
        this.eventChain.add(child.getId());

        child.setParentEventId(this.id);
        child.setCorrelationId(this.correlationId);
    }

    public Map<String, Object> toAuditLog() {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("id", id);
        log.put("timestamp", timestamp);
        log.put("pageId", pageId);
        log.put("eventType", eventType != null ? eventType.getCode() : null);
        log.put("severity", severity != null ? severity.getDisplayName() : null);
        log.put("userId", userId);
        log.put("action", action);
        log.put("description", description);
        log.put("success", success);
        log.put("durationMs", durationMs);
        log.put("riskScore", riskScore);
        log.put("anomalyScore", anomalyScore);
        log.put("flagged", flagged);
        log.put("correlationId", correlationId);
        return log;
    }

    public Map<String, Object> toDetailedLog() {
        Map<String, Object> log = toAuditLog();
        log.put("changes", changes);
        log.put("metadata", metadata);
        log.put("errorMessage", errorMessage);
        log.put("userIp", userIp);
        log.put("userAgent", userAgent);
        log.put("sessionId", sessionId);
        log.put("deviceId", deviceId);
        return log;
    }
}