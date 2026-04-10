package com.purehome.uicore.repository;

import com.purehome.uicore.model.PageAuditEvent;
import com.purehome.uicore.model.PageAuditEvent.EventType;
import com.purehome.uicore.model.PageAuditEvent.Severity;
import com.purehome.uicore.model.PageAuditEvent.StorageTier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * FAANG-GRADE PAGE AUDIT EVENT REPOSITORY
 *
 * ============================================================================
 * INNOVATION ALGORITHM 1: Real-Time Anomaly Detection (RTAD)
 * ============================================================================
 * - Detects anomalous audit patterns in real-time using sliding window
 * - Implements statistical anomaly scoring with z-score normalization
 * - Uses exponential moving average for trend detection
 * - Automatically flags suspicious event sequences
 *
 * ============================================================================
 * INNOVATION ALGORITHM 2: Event Correlation & Chain Analysis (ECCA)
 * ============================================================================
 * - Correlates related events across time and users
 * - Builds complete event chains for user journeys
 * - Implements graph-based causality detection
 * - Provides root cause analysis for failures
 *
 * ============================================================================
 * INNOVATION ALGORITHM 3: Intelligent Audit Retention & Archival (IARA)
 * ============================================================================
 * - Dynamically determines retention based on event importance
 * - Implements tiered storage (hot/warm/cold/glacier)
 * - Automatically anonymizes PII after retention period
 * - Provides compliance reporting (GDPR, CCPA, SOC2)
 *
 * ============================================================================
 * INNOVATION ALGORITHM 4: Security Threat Intelligence (STI)
 * ============================================================================
 * - Detects security threats in real-time from audit logs
 * - Implements pattern matching for known attack vectors
 * - Provides threat scoring and prioritization
 * - Automatically triggers security alerts for critical events
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Repository
public interface PageAuditEventRepository extends MongoRepository<PageAuditEvent, String>, PageAuditEventRepositoryCustom {

    // =========================================================================
    // Core Audit Operations
    // =========================================================================

    /**
     * Find audit events for a specific page with pagination
     * Optimized for audit trail viewing
     */
    @Query("{ 'pageId': ?0, 'deleted': { $ne: true } }")
    Slice<PageAuditEvent> findByPageId(String pageId, Pageable pageable);

    /**
     * Find audit events by user with pagination
     * Used for user activity monitoring
     */
    @Query("{ 'userId': ?0, 'deleted': { $ne: true } }")
    Slice<PageAuditEvent> findByUserId(String userId, Pageable pageable);

    /**
     * Find audit events by correlation ID
     * Retrieves complete event chain for debugging
     */
    @Query("{ 'correlationId': ?0, 'deleted': { $ne: true } }")
    List<PageAuditEvent> findByCorrelationId(String correlationId);

    /**
     * Find audit events by severity within time range
     * Used for security monitoring dashboards
     */
    @Query("{ 'severity': ?0, 'timestamp': { $gte: ?1, $lte: ?2 }, 'deleted': { $ne: true } }")
    List<PageAuditEvent> findBySeverityAndTimeRange(Severity severity, Instant start, Instant end);

    /**
     * Find flagged events requiring review
     * Returns high-risk events for security review
     */
    @Query("{ 'flagged': true, 'requiresReview': true, 'reviewedBy': null, 'deleted': { $ne: true } }")
    List<PageAuditEvent> findEventsRequiringReview();

    // =========================================================================
    // Real-Time Anomaly Detection Queries
    // =========================================================================

    /**
     * Find events with high anomaly scores
     * Used by anomaly detection engine
     */
    @Query("{ 'anomalyScore': { $gt: ?0 }, 'timestamp': { $gt: ?1 }, 'deleted': { $ne: true } }")
    List<PageAuditEvent> findAnomalousEvents(double threshold, Instant since);

    /**
     * Find suspicious login attempts
     * Detects brute force and credential stuffing attacks
     */
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "  'eventType': { $in: ['LOGIN_FAILURE', 'LOGIN_SUCCESS'] }, " +
                    "  'timestamp': { $gt: ?0 }, " +
                    "  'deleted': { $ne: true } " +
                    "} }",
            "{ '$group': { " +
                    "  '_id': '$userId', " +
                    "  'attempts': { '$sum': 1 }, " +
                    "  'failures': { '$sum': { '$cond': [ { '$eq': ['$eventType', 'LOGIN_FAILURE'] }, 1, 0 ] } }, " +
                    "  'successes': { '$sum': { '$cond': [ { '$eq': ['$eventType', 'LOGIN_SUCCESS'] }, 1, 0 ] } }, " +
                    "  'firstAttempt': { '$min': '$timestamp' }, " +
                    "  'lastAttempt': { '$max': '$timestamp' } " +
                    "} }",
            "{ '$match': { 'failures': { $gt: 5 } } }",
            "{ '$sort': { 'failures': -1 } }"
    })
    List<SuspiciousLoginReport> findSuspiciousLogins(Instant since);

    /**
     * Detect rapid succession of events (potential bot activity)
     * Uses sliding window to detect high-frequency events
     */
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "  'userId': { $ne: null }, " +
                    "  'timestamp': { $gt: ?0 }, " +
                    "  'deleted': { $ne: true } " +
                    "} }",
            "{ '$sort': { 'timestamp': 1 } }",
            "{ '$group': { " +
                    "  '_id': '$userId', " +
                    "  'events': { '$push': '$$ROOT' }, " +
                    "  'count': { '$sum': 1 } " +
                    "} }",
            "{ '$project': { " +
                    "  'userId': '$_id', " +
                    "  'eventCount': '$count', " +
                    "  'frequencyScore': { '$divide': ['$count', { '$subtract': [ { '$max': '$events.timestamp' }, { '$min': '$events.timestamp' } ] } ] } " +
                    "} }",
            "{ '$match': { 'frequencyScore': { $gt: 10 } } }" // > 10 events per second
    })
    List<HighFrequencyReport> findHighFrequencyEvents(Instant since);

    // =========================================================================
    // Event Correlation & Chain Analysis
    // =========================================================================

    /**
     * Find event chains for a specific correlation ID
     * Returns complete event hierarchy
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'correlationId': ?0, 'deleted': { $ne: true } } }",
            "{ '$graphLookup': { " +
                    "  'from': 'page_audit_events', " +
                    "  'startWith': '$_id', " +
                    "  'connectFromField': '_id', " +
                    "  'connectToField': 'parentEventId', " +
                    "  'as': 'childEvents', " +
                    "  'maxDepth': 10 " +
                    "} }",
            "{ '$sort': { 'timestamp': 1 } }"
    })
    List<EventChainResult> findEventChains(String correlationId);

    /**
     * Find events related to a specific page operation
     * Traces complete operation lifecycle
     */
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "  'pageId': ?0, " +
                    "  'timestamp': { $gte: ?1, $lte: ?2 }, " +
                    "  'deleted': { $ne: true } " +
                    "} }",
            "{ '$group': { " +
                    "  '_id': '$correlationId', " +
                    "  'events': { '$push': '$$ROOT' }, " +
                    "  'startTime': { '$min': '$timestamp' }, " +
                    "  'endTime': { '$max': '$timestamp' }, " +
                    "  'eventTypes': { '$addToSet': '$eventType' } " +
                    "} }",
            "{ '$match': { 'eventTypes': { $in: ['PAGE_CREATED', 'PAGE_UPDATED', 'PAGE_PUBLISHED'] } } }",
            "{ '$sort': { 'startTime': 1 } }"
    })
    List<OperationChain> findOperationChains(String pageId, Instant start, Instant end);

    // =========================================================================
    // Security Threat Intelligence
    // =========================================================================

    /**
     * Detect potential brute force attacks
     * Analyzes login failure patterns
     */
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "  'eventType': 'LOGIN_FAILURE', " +
                    "  'timestamp': { $gt: ?0 }, " +
                    "  'deleted': { $ne: true } " +
                    "} }",
            "{ '$group': { " +
                    "  '_id': { 'ip': '$userIp', 'userId': '$userId' }, " +
                    "  'attempts': { '$sum': 1 }, " +
                    "  'firstAttempt': { '$min': '$timestamp' }, " +
                    "  'lastAttempt': { '$max': '$timestamp' } " +
                    "} }",
            "{ '$match': { 'attempts': { $gt: 10 } } }",
            "{ '$addFields': { " +
                    "  'threatScore': { '$multiply': ['$attempts', { '$divide': [1, { '$subtract': ['$lastAttempt', '$firstAttempt'] } ] } ] } " +
                    "} }",
            "{ '$sort': { 'threatScore': -1 } }"
    })
    List<ThreatReport> detectBruteForceAttacks(Instant since);

    /**
     * Detect privilege escalation attempts
     * Monitors permission changes and access patterns
     */
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "  '$or': [ " +
                    "    { 'eventType': 'PERMISSION_CHANGED' }, " +
                    "    { 'eventType': 'ROLE_ASSIGNED' }, " +
                    "    { 'eventType': 'ACCESS_DENIED' } " +
                    "  ], " +
                    "  'timestamp': { $gt: ?0 }, " +
                    "  'deleted': { $ne: true } " +
                    "} }",
            "{ '$group': { " +
                    "  '_id': '$userId', " +
                    "  'permissionChanges': { '$sum': { '$cond': [ { '$eq': ['$eventType', 'PERMISSION_CHANGED'] }, 1, 0 ] } }, " +
                    "  'roleChanges': { '$sum': { '$cond': [ { '$eq': ['$eventType', 'ROLE_ASSIGNED'] }, 1, 0 ] } }, " +
                    "  'accessDenied': { '$sum': { '$cond': [ { '$eq': ['$eventType', 'ACCESS_DENIED'] }, 1, 0 ] } } " +
                    "} }",
            "{ '$match': { '$or': [ " +
                    "  { 'permissionChanges': { $gt: 5 } }, " +
                    "  { 'roleChanges': { $gt: 3 } }, " +
                    "  { 'accessDenied': { $gt: 10 } } " +
                    "] } }"
    })
    List<PrivilegeEscalationReport> detectPrivilegeEscalation(Instant since);

    /**
     * Detect unusual access patterns (time-based anomalies)
     * Identifies access outside normal working hours
     */
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "  'timestamp': { $gt: ?0 }, " +
                    "  'deleted': { $ne: true } " +
                    "} }",
            "{ '$project': { " +
                    "  'userId': 1, " +
                    "  'eventType': 1, " +
                    "  'hourOfDay': { '$hour': '$timestamp' }, " +
                    "  'dayOfWeek': { '$dayOfWeek': '$timestamp' } " +
                    "} }",
            "{ '$group': { " +
                    "  '_id': { 'userId': '$userId', 'hour': '$hourOfDay', 'day': '$dayOfWeek' }, " +
                    "  'count': { '$sum': 1 } " +
                    "} }",
            "{ '$match': { " +
                    "  '_id.hour': { $in: [0, 1, 2, 3, 4, 5] }, " +
                    "  '_id.day': { $in: [1, 7] } " +
                    "} }" // Weekend or off-hours
    })
    List<TimeAnomalyReport> detectTimeAnomalies(Instant since);

    // =========================================================================
    // Audit Retention & Archival
    // =========================================================================

    /**
     * Find events ready for archival based on retention policy
     * Used by archival scheduler
     */
    @Query("{ 'retentionDays': { $ne: null }, " +
            "'timestamp': { $lt: { $subtract: [ '$$NOW', { $multiply: ['$retentionDays', 86400000] } ] } }, " +
            "'archived': false, 'deleted': { $ne: true } }")
    List<PageAuditEvent> findEventsReadyForArchival();

    /**
     * Find events by storage tier
     * Used for tiered storage management
     */
    @Query("{ 'storageTier': ?0, 'archived': ?1, 'deleted': { $ne: true } }")
    List<PageAuditEvent> findByStorageTier(StorageTier tier, boolean archived);

    /**
     * Update storage tier for event
     * Moves events between hot/warm/cold storage
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'storageTier': ?1, 'archivedAt': ?2, 'archived': true } }")
    void updateStorageTier(String eventId, StorageTier tier, Instant archivedAt);

    /**
     * Find events ready for PII anonymization
     * Based on retention period and compliance requirements
     */
    @Query("{ 'anonymized': false, " +
            "'timestamp': { $lt: { $subtract: [ '$$NOW', 7776000000 ] } }, " + // 90 days
            "'deleted': { $ne: true } }")
    List<PageAuditEvent> findEventsReadyForAnonymization();

    /**
     * Mark events as anonymized after PII removal
     */
    @Query("{ '_id': { $in: ?0 } }")
    @Update("{ '$set': { 'anonymized': true, 'anonymizedAt': ?1, 'userEmail': 'ANONYMIZED', 'userIp': 'ANONYMIZED' } }")
    void markEventsAnonymized(Set<String> eventIds, Instant anonymizedAt);

    // =========================================================================
    // Analytics & Reporting
    // =========================================================================

    /**
     * Get audit summary for dashboard
     * Aggregates event counts by type and severity
     */
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "  'timestamp': { $gte: ?0, $lte: ?1 }, " +
                    "  'deleted': { $ne: true } " +
                    "} }",
            "{ '$group': { " +
                    "  '_id': { 'eventType': '$eventType', 'severity': '$severity' }, " +
                    "  'count': { '$sum': 1 }, " +
                    "  'uniqueUsers': { '$addToSet': '$userId' }, " +
                    "  'uniquePages': { '$addToSet': '$pageId' } " +
                    "} }",
            "{ '$project': { " +
                    "  'eventType': '$_id.eventType', " +
                    "  'severity': '$_id.severity', " +
                    "  'count': 1, " +
                    "  'uniqueUserCount': { '$size': '$uniqueUsers' }, " +
                    "  'uniquePageCount': { '$size': '$uniquePages' } " +
                    "} }",
            "{ '$sort': { 'count': -1 } }"
    })
    List<AuditSummary> getAuditSummary(Instant start, Instant end);

    /**
     * Get user activity report
     * Aggregates user actions for compliance reporting
     */
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "  'userId': ?0, " +
                    "  'timestamp': { $gte: ?1, $lte: ?2 }, " +
                    "  'deleted': { $ne: true } " +
                    "} }",
            "{ '$group': { " +
                    "  '_id': { 'date': { '$dateToString': { 'format': '%Y-%m-%d', 'date': '$timestamp' } }, 'eventType': '$eventType' }, " +
                    "  'count': { '$sum': 1 }, " +
                    "  'actions': { '$push': { 'action': '$action', 'pageId': '$pageId', 'timestamp': '$timestamp' } } " +
                    "} }",
            "{ '$sort': { '_id.date': -1 } }"
    })
    List<UserActivityReport> getUserActivityReport(String userId, Instant start, Instant end);

    /**
     * Get security incident report
     * Aggregates high-severity events for security team
     */
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "  'severity': { $in: ['ERROR', 'CRITICAL'] }, " +
                    "  'timestamp': { $gte: ?0, $lte: ?1 }, " +
                    "  'deleted': { $ne: true } " +
                    "} }",
            "{ '$group': { " +
                    "  '_id': '$eventType', " +
                    "  'count': { '$sum': 1 }, " +
                    "  'affectedUsers': { '$addToSet': '$userId' }, " +
                    "  'affectedPages': { '$addToSet': '$pageId' }, " +
                    "  'firstOccurrence': { '$min': '$timestamp' }, " +
                    "  'lastOccurrence': { '$max': '$timestamp' } " +
                    "} }",
            "{ '$project': { " +
                    "  'eventType': '$_id', " +
                    "  'count': 1, " +
                    "  'uniqueUsers': { '$size': '$affectedUsers' }, " +
                    "  'uniquePages': { '$size': '$affectedPages' }, " +
                    "  'firstOccurrence': 1, " +
                    "  'lastOccurrence': 1, " +
                    "  'frequency': { '$divide': ['$count', { '$subtract': ['$lastOccurrence', '$firstOccurrence'] } ] } " +
                    "} }",
            "{ '$sort': { 'count': -1 } }"
    })
    List<SecurityIncidentReport> getSecurityIncidentReport(Instant start, Instant end);

    // =========================================================================
    // Compliance & Audit Trail
    // =========================================================================

    /**
     * Export audit trail for compliance
     * Retrieves complete audit trail for a time period
     */
    @Query("{ 'timestamp': { $gte: ?0, $lte: ?1 }, 'deleted': { $ne: true } }")
    List<PageAuditEvent> exportAuditTrail(Instant start, Instant end, Pageable pageable);

    /**
     * Get audit trail for specific page
     * Used for content audit and legal requests
     */
    @Query("{ 'pageId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 }, 'deleted': { $ne: true } }")
    List<PageAuditEvent> getPageAuditTrail(String pageId, Instant start, Instant end);

    /**
     * Count events by type and severity for monitoring
     */
    @Query(value = "{ 'timestamp': { $gt: ?0 }, 'deleted': { $ne: true } }", count = true)
    long countEventsSince(Instant since);

    /**
     * Get storage usage by tier
     * For cost optimization and capacity planning
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'deleted': { $ne: true } } }",
            "{ '$group': { " +
                    "  '_id': '$storageTier', " +
                    "  'count': { '$sum': 1 }, " +
                    "  'oldestEvent': { '$min': '$timestamp' }, " +
                    "  'newestEvent': { '$max': '$timestamp' } " +
                    "} }"
    })
    List<StorageUsageReport> getStorageUsageByTier();
}

// =========================================================================
// Custom Repository Interface with Advanced Audit Algorithms
// =========================================================================

interface PageAuditEventRepositoryCustom {

    /**
     * INNOVATION: Real-time anomaly detection with ML scoring
     * Uses statistical analysis to detect unusual patterns
     */
    AnomalyDetectionResult detectRealTimeAnomalies(PageAuditEvent event, WindowConfig config);

    /**
     * INNOVATION: Build complete event causality graph
     * Creates directed acyclic graph of event relationships
     */
    CausalityGraph buildCausalityGraph(String correlationId, int maxDepth);

    /**
     * INNOVATION: Predict future security threats based on patterns
     * Uses time series analysis to forecast potential incidents
     */
    ThreatPrediction predictThreats(String pageId, PredictionWindow window);

    /**
     * INNOVATION: Calculate risk score for user activity
     * Combines multiple factors to assess user risk level
     */
    RiskScore calculateUserRiskScore(String userId, TimeRange range);

    /**
     * INNOVATION: Generate compliance report for GDPR/CCPA
     * Creates comprehensive audit report for regulatory compliance
     */
    ComplianceReport generateComplianceReport(String userId, ComplianceScope scope);

    /**
     * INNOVATION: Analyze audit patterns for optimization
     * Identifies patterns to optimize storage and retention
     */
    AuditPatternAnalysis analyzeAuditPatterns(TimeRange range);
}

// =========================================================================
// DTO Classes for Complex Results
// =========================================================================

/**
 * Suspicious login report
 */
class SuspiciousLoginReport {
    private String userId;
    private String userIp;
    private int attempts;
    private int failures;
    private int successes;
    private Instant firstAttempt;
    private Instant lastAttempt;
    private double successRate;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserIp() { return userIp; }
    public void setUserIp(String userIp) { this.userIp = userIp; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public int getFailures() { return failures; }
    public void setFailures(int failures) { this.failures = failures; }
    public int getSuccesses() { return successes; }
    public void setSuccesses(int successes) { this.successes = successes; }
    public Instant getFirstAttempt() { return firstAttempt; }
    public void setFirstAttempt(Instant firstAttempt) { this.firstAttempt = firstAttempt; }
    public Instant getLastAttempt() { return lastAttempt; }
    public void setLastAttempt(Instant lastAttempt) { this.lastAttempt = lastAttempt; }
    public double getSuccessRate() { return successes > 0 ? (double) successes / attempts : 0; }
}

/**
 * High frequency event report
 */
class HighFrequencyReport {
    private String userId;
    private long eventCount;
    private double frequencyScore;
    private String timeWindow;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getEventCount() { return eventCount; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }
    public double getFrequencyScore() { return frequencyScore; }
    public void setFrequencyScore(double frequencyScore) { this.frequencyScore = frequencyScore; }
    public String getTimeWindow() { return timeWindow; }
    public void setTimeWindow(String timeWindow) { this.timeWindow = timeWindow; }
}

/**
 * Event chain result
 */
class EventChainResult {
    private PageAuditEvent rootEvent;
    private List<PageAuditEvent> childEvents;
    private int chainDepth;
    private long totalDurationMs;

    // Getters and setters
    public PageAuditEvent getRootEvent() { return rootEvent; }
    public void setRootEvent(PageAuditEvent rootEvent) { this.rootEvent = rootEvent; }
    public List<PageAuditEvent> getChildEvents() { return childEvents; }
    public void setChildEvents(List<PageAuditEvent> childEvents) { this.childEvents = childEvents; }
    public int getChainDepth() { return chainDepth; }
    public void setChainDepth(int chainDepth) { this.chainDepth = chainDepth; }
    public long getTotalDurationMs() { return totalDurationMs; }
    public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
}

/**
 * Operation chain for page lifecycle
 */
class OperationChain {
    private String correlationId;
    private List<PageAuditEvent> events;
    private Instant startTime;
    private Instant endTime;
    private Set<EventType> eventTypes;
    private boolean successful;

    // Getters and setters
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public List<PageAuditEvent> getEvents() { return events; }
    public void setEvents(List<PageAuditEvent> events) { this.events = events; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public Set<EventType> getEventTypes() { return eventTypes; }
    public void setEventTypes(Set<EventType> eventTypes) { this.eventTypes = eventTypes; }
    public boolean isSuccessful() {
        return events.stream().anyMatch(e -> Boolean.TRUE.equals(e.getSuccess()));
    }
}

/**
 * Threat report for security incidents
 */
class ThreatReport {
    private String userIp;
    private String userId;
    private int attempts;
    private Instant firstAttempt;
    private Instant lastAttempt;
    private double threatScore;
    private String threatType;

    // Getters and setters
    public String getUserIp() { return userIp; }
    public void setUserIp(String userIp) { this.userIp = userIp; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getFirstAttempt() { return firstAttempt; }
    public void setFirstAttempt(Instant firstAttempt) { this.firstAttempt = firstAttempt; }
    public Instant getLastAttempt() { return lastAttempt; }
    public void setLastAttempt(Instant lastAttempt) { this.lastAttempt = lastAttempt; }
    public double getThreatScore() { return threatScore; }
    public void setThreatScore(double threatScore) { this.threatScore = threatScore; }
    public String getThreatType() { return threatType; }
    public void setThreatType(String threatType) { this.threatType = threatType; }
}

/**
 * Privilege escalation report
 */
class PrivilegeEscalationReport {
    private String userId;
    private int permissionChanges;
    private int roleChanges;
    private int accessDenied;
    private double riskScore;
    private Instant firstDetected;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getPermissionChanges() { return permissionChanges; }
    public void setPermissionChanges(int permissionChanges) { this.permissionChanges = permissionChanges; }
    public int getRoleChanges() { return roleChanges; }
    public void setRoleChanges(int roleChanges) { this.roleChanges = roleChanges; }
    public int getAccessDenied() { return accessDenied; }
    public void setAccessDenied(int accessDenied) { this.accessDenied = accessDenied; }
    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
    public Instant getFirstDetected() { return firstDetected; }
    public void setFirstDetected(Instant firstDetected) { this.firstDetected = firstDetected; }
}

/**
 * Time anomaly report
 */
class TimeAnomalyReport {
    private String userId;
    private int hourOfDay;
    private int dayOfWeek;
    private long count;
    private double anomalyScore;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(int hourOfDay) { this.hourOfDay = hourOfDay; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }
}

/**
 * Audit summary for dashboard
 */
class AuditSummary {
    private EventType eventType;
    private Severity severity;
    private long count;
    private int uniqueUserCount;
    private int uniquePageCount;

    // Getters and setters
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public int getUniqueUserCount() { return uniqueUserCount; }
    public void setUniqueUserCount(int uniqueUserCount) { this.uniqueUserCount = uniqueUserCount; }
    public int getUniquePageCount() { return uniquePageCount; }
    public void setUniquePageCount(int uniquePageCount) { this.uniquePageCount = uniquePageCount; }
}

/**
 * User activity report
 */
class UserActivityReport {
    private String date;
    private Map<EventType, Integer> activityByType;
    private List<UserAction> actions;

    // Getters and setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Map<EventType, Integer> getActivityByType() { return activityByType; }
    public void setActivityByType(Map<EventType, Integer> activityByType) { this.activityByType = activityByType; }
    public List<UserAction> getActions() { return actions; }
    public void setActions(List<UserAction> actions) { this.actions = actions; }
}

/**
 * User action detail
 */
class UserAction {
    private String action;
    private String pageId;
    private Instant timestamp;

    // Getters and setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

/**
 * Security incident report
 */
class SecurityIncidentReport {
    private EventType eventType;
    private long count;
    private int uniqueUsers;
    private int uniquePages;
    private Instant firstOccurrence;
    private Instant lastOccurrence;
    private double frequency;

    // Getters and setters
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public int getUniqueUsers() { return uniqueUsers; }
    public void setUniqueUsers(int uniqueUsers) { this.uniqueUsers = uniqueUsers; }
    public int getUniquePages() { return uniquePages; }
    public void setUniquePages(int uniquePages) { this.uniquePages = uniquePages; }
    public Instant getFirstOccurrence() { return firstOccurrence; }
    public void setFirstOccurrence(Instant firstOccurrence) { this.firstOccurrence = firstOccurrence; }
    public Instant getLastOccurrence() { return lastOccurrence; }
    public void setLastOccurrence(Instant lastOccurrence) { this.lastOccurrence = lastOccurrence; }
    public double getFrequency() { return frequency; }
    public void setFrequency(double frequency) { this.frequency = frequency; }
}

/**
 * Storage usage report
 */
class StorageUsageReport {
    private StorageTier storageTier;
    private long count;
    private Instant oldestEvent;
    private Instant newestEvent;

    // Getters and setters
    public StorageTier getStorageTier() { return storageTier; }
    public void setStorageTier(StorageTier storageTier) { this.storageTier = storageTier; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public Instant getOldestEvent() { return oldestEvent; }
    public void setOldestEvent(Instant oldestEvent) { this.oldestEvent = oldestEvent; }
    public Instant getNewestEvent() { return newestEvent; }
    public void setNewestEvent(Instant newestEvent) { this.newestEvent = newestEvent; }
}

/**
 * Window configuration for anomaly detection
 */
class WindowConfig {
    private int windowSizeMinutes;
    private double zScoreThreshold;
    private boolean useExponentialMovingAverage;

    public static WindowConfig defaultConfig() {
        WindowConfig config = new WindowConfig();
        config.windowSizeMinutes = 60;
        config.zScoreThreshold = 2.5;
        config.useExponentialMovingAverage = true;
        return config;
    }

    // Getters and setters
    public int getWindowSizeMinutes() { return windowSizeMinutes; }
    public void setWindowSizeMinutes(int windowSizeMinutes) { this.windowSizeMinutes = windowSizeMinutes; }
    public double getzScoreThreshold() { return zScoreThreshold; }
    public void setzScoreThreshold(double zScoreThreshold) { this.zScoreThreshold = zScoreThreshold; }
    public boolean isUseExponentialMovingAverage() { return useExponentialMovingAverage; }
    public void setUseExponentialMovingAverage(boolean useExponentialMovingAverage) { this.useExponentialMovingAverage = useExponentialMovingAverage; }
}

/**
 * Anomaly detection result
 */
class AnomalyDetectionResult {
    private boolean isAnomaly;
    private double anomalyScore;
    private String detectedPattern;
    private List<String> contributingFactors;
    private Instant detectionTime;

    // Getters and setters
    public boolean isAnomaly() { return isAnomaly; }
    public void setAnomaly(boolean anomaly) { isAnomaly = anomaly; }
    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }
    public String getDetectedPattern() { return detectedPattern; }
    public void setDetectedPattern(String detectedPattern) { this.detectedPattern = detectedPattern; }
    public List<String> getContributingFactors() { return contributingFactors; }
    public void setContributingFactors(List<String> contributingFactors) { this.contributingFactors = contributingFactors; }
    public Instant getDetectionTime() { return detectionTime; }
    public void setDetectionTime(Instant detectionTime) { this.detectionTime = detectionTime; }
}

/**
 * Causality graph for event relationships
 */
class CausalityGraph {
    private String rootEventId;
    private Map<String, List<String>> edges;
    private Map<String, PageAuditEvent> nodes;
    private int totalNodes;
    private int totalEdges;

    // Getters and setters
    public String getRootEventId() { return rootEventId; }
    public void setRootEventId(String rootEventId) { this.rootEventId = rootEventId; }
    public Map<String, List<String>> getEdges() { return edges; }
    public void setEdges(Map<String, List<String>> edges) { this.edges = edges; }
    public Map<String, PageAuditEvent> getNodes() { return nodes; }
    public void setNodes(Map<String, PageAuditEvent> nodes) { this.nodes = nodes; }
    public int getTotalNodes() { return totalNodes; }
    public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }
    public int getTotalEdges() { return totalEdges; }
    public void setTotalEdges(int totalEdges) { this.totalEdges = totalEdges; }
}

/**
 * Threat prediction result
 */
class ThreatPrediction {
    private double threatProbability;
    private List<PredictedThreat> predictedThreats;
    private Instant predictionWindow;
    private String confidenceLevel;

    // Getters and setters
    public double getThreatProbability() { return threatProbability; }
    public void setThreatProbability(double threatProbability) { this.threatProbability = threatProbability; }
    public List<PredictedThreat> getPredictedThreats() { return predictedThreats; }
    public void setPredictedThreats(List<PredictedThreat> predictedThreats) { this.predictedThreats = predictedThreats; }
    public Instant getPredictionWindow() { return predictionWindow; }
    public void setPredictionWindow(Instant predictionWindow) { this.predictionWindow = predictionWindow; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
}

/**
 * Predicted threat detail
 */
class PredictedThreat {
    private String threatType;
    private double probability;
    private String description;
    private List<String> mitigationSteps;

    // Getters and setters
    public String getThreatType() { return threatType; }
    public void setThreatType(String threatType) { this.threatType = threatType; }
    public double getProbability() { return probability; }
    public void setProbability(double probability) { this.probability = probability; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getMitigationSteps() { return mitigationSteps; }
    public void setMitigationSteps(List<String> mitigationSteps) { this.mitigationSteps = mitigationSteps; }
}

/**
 * Risk score for user
 */
class RiskScore {
    private String userId;
    private double overallRisk;
    private Map<String, Double> riskFactors;
    private String riskLevel;
    private List<String> riskReasons;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public double getOverallRisk() { return overallRisk; }
    public void setOverallRisk(double overallRisk) { this.overallRisk = overallRisk; }
    public Map<String, Double> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(Map<String, Double> riskFactors) { this.riskFactors = riskFactors; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<String> getRiskReasons() { return riskReasons; }
    public void setRiskReasons(List<String> riskReasons) { this.riskReasons = riskReasons; }
}

/**
 * Time range for analysis
 */
class TimeRange {
    private Instant start;
    private Instant end;

    public TimeRange(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    // Getters and setters
    public Instant getStart() { return start; }
    public void setStart(Instant start) { this.start = start; }
    public Instant getEnd() { return end; }
    public void setEnd(Instant end) { this.end = end; }
}

/**
 * Prediction window for threat forecasting
 */
class PredictionWindow {
    private int hoursAhead;
    private int historicalDays;

    public static PredictionWindow defaultConfig() {
        PredictionWindow window = new PredictionWindow();
        window.hoursAhead = 24;
        window.historicalDays = 30;
        return window;
    }

    // Getters and setters
    public int getHoursAhead() { return hoursAhead; }
    public void setHoursAhead(int hoursAhead) { this.hoursAhead = hoursAhead; }
    public int getHistoricalDays() { return historicalDays; }
    public void setHistoricalDays(int historicalDays) { this.historicalDays = historicalDays; }
}

/**
 * Compliance scope for GDPR/CCPA reports
 */
class ComplianceScope {
    private boolean includePersonalData;
    private boolean includeIPAddresses;
    private boolean includeUserAgent;
    private boolean includeActionDetails;
    private int retentionDays;

    public static ComplianceScope gdprFull() {
        ComplianceScope scope = new ComplianceScope();
        scope.includePersonalData = true;
        scope.includeIPAddresses = true;
        scope.includeUserAgent = true;
        scope.includeActionDetails = true;
        scope.retentionDays = 30;
        return scope;
    }

    // Getters and setters
    public boolean isIncludePersonalData() { return includePersonalData; }
    public void setIncludePersonalData(boolean includePersonalData) { this.includePersonalData = includePersonalData; }
    public boolean isIncludeIPAddresses() { return includeIPAddresses; }
    public void setIncludeIPAddresses(boolean includeIPAddresses) { this.includeIPAddresses = includeIPAddresses; }
    public boolean isIncludeUserAgent() { return includeUserAgent; }
    public void setIncludeUserAgent(boolean includeUserAgent) { this.includeUserAgent = includeUserAgent; }
    public boolean isIncludeActionDetails() { return includeActionDetails; }
    public void setIncludeActionDetails(boolean includeActionDetails) { this.includeActionDetails = includeActionDetails; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
}

/**
 * Compliance report for regulatory requirements
 */
class ComplianceReport {
    private String userId;
    private TimeRange reportPeriod;
    private List<PageAuditEvent> events;
    private Map<String, Object> personalData;
    private boolean dataAnonymized;
    private String exportFormat;
    private Instant generatedAt;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public TimeRange getReportPeriod() { return reportPeriod; }
    public void setReportPeriod(TimeRange reportPeriod) { this.reportPeriod = reportPeriod; }
    public List<PageAuditEvent> getEvents() { return events; }
    public void setEvents(List<PageAuditEvent> events) { this.events = events; }
    public Map<String, Object> getPersonalData() { return personalData; }
    public void setPersonalData(Map<String, Object> personalData) { this.personalData = personalData; }
    public boolean isDataAnonymized() { return dataAnonymized; }
    public void setDataAnonymized(boolean dataAnonymized) { this.dataAnonymized = dataAnonymized; }
    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}

/**
 * Audit pattern analysis result
 */
class AuditPatternAnalysis {
    private Map<String, Long> eventFrequencyPatterns;
    private List<String> recurringPatterns;
    private List<String> optimizationRecommendations;
    private Map<String, Double> storageOptimizationPotential;
    private long totalEventsAnalyzed;

    // Getters and setters
    public Map<String, Long> getEventFrequencyPatterns() { return eventFrequencyPatterns; }
    public void setEventFrequencyPatterns(Map<String, Long> eventFrequencyPatterns) { this.eventFrequencyPatterns = eventFrequencyPatterns; }
    public List<String> getRecurringPatterns() { return recurringPatterns; }
    public void setRecurringPatterns(List<String> recurringPatterns) { this.recurringPatterns = recurringPatterns; }
    public List<String> getOptimizationRecommendations() { return optimizationRecommendations; }
    public void setOptimizationRecommendations(List<String> optimizationRecommendations) { this.optimizationRecommendations = optimizationRecommendations; }
    public Map<String, Double> getStorageOptimizationPotential() { return storageOptimizationPotential; }
    public void setStorageOptimizationPotential(Map<String, Double> storageOptimizationPotential) { this.storageOptimizationPotential = storageOptimizationPotential; }
    public long getTotalEventsAnalyzed() { return totalEventsAnalyzed; }
    public void setTotalEventsAnalyzed(long totalEventsAnalyzed) { this.totalEventsAnalyzed = totalEventsAnalyzed; }
}