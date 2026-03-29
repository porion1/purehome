package com.purehome.uicore.controller;

import com.purehome.uicore.dto.response.ApiResponse;
import com.purehome.uicore.dto.response.AuditEventResponse;
import com.purehome.uicore.dto.response.AuditAnalyticsResponse;
import com.purehome.uicore.dto.response.ComplianceReportResponse;
import com.purehome.uicore.model.PageAuditEvent.EventType;
import com.purehome.uicore.model.PageAuditEvent.Severity;
import com.purehome.uicore.service.PageAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Management", description = "APIs for audit logging, anomaly detection, and compliance reporting")
@SecurityRequirement(name = "bearerAuth")
public class PageAuditController {

    private final PageAuditService auditService;

    // =========================================================================
    // Core Audit Operations
    // =========================================================================

    @GetMapping("/pages/{pageId}")
    @Operation(summary = "Get page audit trail", description = "Retrieves audit trail for a specific page")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<PageAuditService.AuditTrailResponse>> getPageAuditTrail(
            @PathVariable String pageId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {

        log.debug("Fetching audit trail for page: {}", pageId);

        PageAuditService.AuditTrailResponse trail = auditService.getPageAuditTrail(
                pageId, startDate, endDate, eventType, severity, cursor, limit);

        return ResponseEntity.ok(ApiResponse.success(trail, "Audit trail retrieved successfully"));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user activity timeline", description = "Retrieves activity timeline for a specific user")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<PageAuditService.UserActivityTimeline>> getUserActivityTimeline(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        log.debug("Fetching user activity timeline for user: {}", userId);

        PageAuditService.UserActivityTimeline timeline = auditService.getUserActivityTimeline(userId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(timeline, "User activity timeline retrieved successfully"));
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get audit event", description = "Retrieves a specific audit event by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<AuditEventResponse>> getAuditEvent(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "false") boolean verifyIntegrity) {

        log.debug("Fetching audit event: {}", eventId);

        Optional<AuditEventResponse> event = auditService.getAuditEvent(eventId, verifyIntegrity);

        return event.map(e -> ResponseEntity.ok(ApiResponse.success(e, "Audit event retrieved successfully")))
                .orElseThrow(() -> new RuntimeException("Audit event not found: " + eventId));
    }

    // =========================================================================
    // Anomaly Detection
    // =========================================================================

    @GetMapping("/anomalies/{eventId}")
    @Operation(summary = "Detect anomaly", description = "Detects anomalies in a specific audit event")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<PageAuditService.AnomalyDetectionResult>> detectAnomaly(
            @PathVariable String eventId) {

        log.debug("Detecting anomaly for event: {}", eventId);

        PageAuditService.AnomalyDetectionResult result = auditService.detectAnomaly(eventId);

        return ResponseEntity.ok(ApiResponse.success(result, "Anomaly detection completed"));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get active alerts", description = "Retrieves active security alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PageAuditService.SecurityAlert>>> getActiveAlerts(
            @RequestParam(required = false) Severity severity,
            @RequestParam(defaultValue = "50") int limit) {

        log.debug("Fetching active alerts");

        List<PageAuditService.SecurityAlert> alerts = auditService.getActiveAlerts(severity, limit);

        return ResponseEntity.ok(ApiResponse.success(alerts, "Active alerts retrieved successfully"));
    }

    @PostMapping("/alerts/{alertId}/resolve")
    @Operation(summary = "Resolve alert", description = "Resolves a security alert")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resolveAlert(
            @PathVariable String alertId,
            @RequestParam String resolution,
            @RequestParam String actionTaken,
            @RequestAttribute("userId") String userId) {

        log.info("Resolving alert: {} by user: {}", alertId, userId);

        auditService.resolveAlert(alertId, userId, resolution, actionTaken);

        return ResponseEntity.ok(ApiResponse.success(null, "Alert resolved successfully"));
    }

    // =========================================================================
    // Threat Intelligence
    // =========================================================================

    @GetMapping("/threats")
    @Operation(summary = "Detect security threats", description = "Detects security threats in audit stream")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PageAuditService.SecurityThreat>>> detectSecurityThreats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        log.debug("Detecting security threats from {} to {}", startTime, endTime);

        List<PageAuditService.SecurityThreat> threats = auditService.detectSecurityThreats(startTime, endTime);

        return ResponseEntity.ok(ApiResponse.success(threats, "Security threat detection completed"));
    }

    @GetMapping("/threats/intelligence")
    @Operation(summary = "Get threat intelligence", description = "Retrieves threat intelligence summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.ThreatIntelligence>> getThreatIntelligence(
            @RequestParam(defaultValue = "30") int days) {

        log.debug("Fetching threat intelligence for last {} days", days);

        PageAuditService.ThreatIntelligence intelligence = auditService.getThreatIntelligence(days);

        return ResponseEntity.ok(ApiResponse.success(intelligence, "Threat intelligence retrieved successfully"));
    }

    @GetMapping("/users/{userId}/risk")
    @Operation(summary = "Analyze user risk", description = "Analyzes risk score for a specific user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.UserRiskScore>> analyzeUserRisk(
            @PathVariable String userId,
            @RequestParam(defaultValue = "30") int days) {

        log.debug("Analyzing user risk for: {} over {} days", userId, days);

        PageAuditService.UserRiskScore risk = auditService.analyzeUserRisk(userId, days);

        return ResponseEntity.ok(ApiResponse.success(risk, "User risk analysis completed"));
    }

    @GetMapping("/threats/insider")
    @Operation(summary = "Detect insider threats", description = "Detects potential insider threats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PageAuditService.InsiderThreat>>> detectInsiderThreats(
            @RequestParam(defaultValue = "0.7") double threshold) {

        log.debug("Detecting insider threats with threshold: {}", threshold);

        List<PageAuditService.InsiderThreat> threats = auditService.detectInsiderThreats(threshold);

        return ResponseEntity.ok(ApiResponse.success(threats, "Insider threat detection completed"));
    }

    // =========================================================================
    // Compliance Reporting
    // =========================================================================

    @GetMapping("/compliance/gdpr/{userId}")
    @Operation(summary = "Generate GDPR report", description = "Generates GDPR compliance report for a user")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<ApiResponse<ComplianceReportResponse>>> generateGDPRReport(
            @PathVariable String userId,
            @RequestParam(defaultValue = "JSON") String format) {

        log.info("Generating GDPR report for user: {} in format: {}", userId, format);

        return auditService.generateGDPRReport(userId, format)
                .thenApply(report -> ResponseEntity.ok(ApiResponse.success(report, "GDPR report generated successfully")));
    }

    @GetMapping("/compliance/soc2")
    @Operation(summary = "Generate SOC2 report", description = "Generates SOC2 compliance report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> generateSOC2Report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "SUMMARY") String reportType) {

        log.info("Generating SOC2 report from {} to {}, type: {}", startDate, endDate, reportType);

        ComplianceReportResponse report = auditService.generateSOC2Report(startDate, endDate, reportType);

        return ResponseEntity.ok(ApiResponse.success(report, "SOC2 report generated successfully"));
    }

    @GetMapping("/compliance/ccpa/{userId}")
    @Operation(summary = "Generate CCPA report", description = "Generates CCPA compliance report for a user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.CCPAResponse>> generateCCPAReport(
            @PathVariable String userId,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        log.info("Generating CCPA report for user: {}", userId);

        PageAuditService.CCPAResponse report = auditService.generateCCPAReport(userId, includeDeleted);

        return ResponseEntity.ok(ApiResponse.success(report, "CCPA report generated successfully"));
    }

    @DeleteMapping("/compliance/users/{userId}")
    @Operation(summary = "Delete user data", description = "Deletes user data for GDPR compliance (Right to be Forgotten)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.DataDeletionResult>> deleteUserData(
            @PathVariable String userId,
            @RequestParam String verificationCode,
            @RequestAttribute("userId") String requestor) {

        log.info("Deleting user data for: {} by requestor: {}", userId, requestor);

        PageAuditService.DataDeletionResult result = auditService.deleteUserData(userId, requestor, verificationCode);

        return ResponseEntity.ok(ApiResponse.success(result, "User data deletion completed"));
    }

    // =========================================================================
    // Analytics & Insights
    // =========================================================================

    @GetMapping("/analytics")
    @Operation(summary = "Get audit analytics", description = "Retrieves comprehensive audit analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuditAnalyticsResponse>> getAuditAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "DAY") String groupBy) {

        log.debug("Getting audit analytics from {} to {}, group by: {}", startDate, endDate, groupBy);

        AuditAnalyticsResponse analytics = auditService.getAuditAnalytics(startDate, endDate, groupBy);

        return ResponseEntity.ok(ApiResponse.success(analytics, "Audit analytics retrieved successfully"));
    }

    @GetMapping("/analytics/behavior")
    @Operation(summary = "Get user behavior analytics", description = "Retrieves user behavior analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.UserBehaviorAnalytics>> getUserBehaviorAnalytics(
            @RequestParam String workspaceId,
            @RequestParam(defaultValue = "30") int days) {

        log.debug("Getting user behavior analytics for workspace: {} over {} days", workspaceId, days);

        PageAuditService.UserBehaviorAnalytics analytics = auditService.getUserBehaviorAnalytics(workspaceId, days);

        return ResponseEntity.ok(ApiResponse.success(analytics, "User behavior analytics retrieved successfully"));
    }

    @GetMapping("/analytics/health")
    @Operation(summary = "Get system health insights", description = "Retrieves system health insights from audit logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.SystemHealthInsights>> getSystemHealthInsights(
            @RequestParam(defaultValue = "7") int days) {

        log.debug("Getting system health insights for last {} days", days);

        PageAuditService.SystemHealthInsights insights = auditService.getSystemHealthInsights(days);

        return ResponseEntity.ok(ApiResponse.success(insights, "System health insights retrieved successfully"));
    }

    // =========================================================================
    // Export & Retention
    // =========================================================================

    @PostMapping("/export")
    @Operation(summary = "Export audit data", description = "Exports audit data for external analysis")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> exportAuditData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "JSON") String format,
            @RequestParam(required = false) List<String> fields) {

        log.info("Exporting audit data from {} to {} in format: {}", startDate, endDate, format);

        String exportId = auditService.exportAuditData(startDate, endDate, format, fields);

        return ResponseEntity.ok(ApiResponse.success(exportId, "Export job started successfully"));
    }

    @GetMapping("/export/{exportId}")
    @Operation(summary = "Get export status", description = "Gets the status of an export job")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.ExportStatus>> getExportStatus(
            @PathVariable String exportId) {

        log.debug("Getting export status for: {}", exportId);

        PageAuditService.ExportStatus status = auditService.getExportStatus(exportId);

        return ResponseEntity.ok(ApiResponse.success(status, "Export status retrieved successfully"));
    }

    @PostMapping("/retention/apply")
    @Operation(summary = "Apply retention policy", description = "Applies retention policy to audit data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.RetentionResult>> applyRetentionPolicy(
            @RequestParam(required = false) String workspaceId,
            @RequestParam(defaultValue = "false") boolean dryRun) {

        log.info("Applying retention policy for workspace: {} (dryRun: {})", workspaceId, dryRun);

        PageAuditService.RetentionResult result = auditService.applyRetentionPolicy(workspaceId, dryRun);

        return ResponseEntity.ok(ApiResponse.success(result, "Retention policy applied successfully"));
    }

    @GetMapping("/storage/metrics")
    @Operation(summary = "Get storage metrics", description = "Retrieves audit storage metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.StorageMetrics>> getStorageMetrics() {

        log.debug("Getting storage metrics");

        PageAuditService.StorageMetrics metrics = auditService.getStorageMetrics();

        return ResponseEntity.ok(ApiResponse.success(metrics, "Storage metrics retrieved successfully"));
    }

    @PostMapping("/archive")
    @Operation(summary = "Archive audit data", description = "Archives old audit data to cold storage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.ArchiveResult>> archiveAuditData(
            @RequestParam(defaultValue = "365") int daysBeforeArchive,
            @RequestParam(required = false) String workspaceId) {

        log.info("Archiving audit data older than {} days", daysBeforeArchive);

        PageAuditService.ArchiveResult result = auditService.archiveAuditData(daysBeforeArchive, workspaceId);

        return ResponseEntity.ok(ApiResponse.success(result, "Audit archival completed successfully"));
    }

    @PostMapping("/restore/{archiveId}")
    @Operation(summary = "Restore archived data", description = "Restores archived audit data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageAuditService.RestoreResult>> restoreArchivedData(
            @PathVariable String archiveId,
            @RequestAttribute("userId") String userId) {

        log.info("Restoring archived data: {} by user: {}", archiveId, userId);

        PageAuditService.RestoreResult result = auditService.restoreArchivedData(archiveId, userId);

        return ResponseEntity.ok(ApiResponse.success(result, "Archived data restored successfully"));
    }
}