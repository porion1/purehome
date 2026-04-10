package com.purehome.uicore.controller;

import com.purehome.uicore.dto.request.PublishPageRequest;
import com.purehome.uicore.dto.response.PublishStatusResponse;
import com.purehome.uicore.service.PagePublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/publish")
@RequiredArgsConstructor
@Tag(name = "Page Publishing", description = "APIs for publishing, canary deployments, and batch publishing")
@SecurityRequirement(name = "bearerAuth")
public class PagePublishController {

    private final PagePublishService publishService;

    // =========================================================================
    // Core Publishing Operations
    // =========================================================================

    @PostMapping("/pages/{pageId}")
    @Operation(summary = "Publish page", description = "Publishes a page immediately or scheduled")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public CompletableFuture<ResponseEntity<PublishStatusResponse>> publishPage(
            @PathVariable String pageId,
            @Valid @RequestBody PublishPageRequest request,
            @RequestAttribute("userId") String userId) {

        log.info("Publishing page: {} by user: {}", pageId, userId);

        Instant publishTime = request.isImmediate() ? null : request.getPublishTime();
        PagePublishService.PublishOptions options = buildPublishOptions(request);

        return publishService.publishPage(pageId, userId, publishTime, request.getTimezone(), options)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/pages/{pageId}/unpublish")
    @Operation(summary = "Unpublish page", description = "Unpublishes a page immediately or scheduled")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public CompletableFuture<ResponseEntity<PublishStatusResponse>> unpublishPage(
            @PathVariable String pageId,
            @RequestParam(required = false) Instant unpublishTime,
            @RequestParam(required = false) String reason,
            @RequestAttribute("userId") String userId) {

        log.info("Unpublishing page: {} by user: {}", pageId, userId);

        return publishService.unpublishPage(pageId, userId, unpublishTime, reason != null ? reason : "No reason provided")
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/pages/{pageId}/schedule")
    @Operation(summary = "Schedule publish", description = "Schedules a page for future publishing")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PagePublishService.ScheduleResult> schedulePublish(
            @PathVariable String pageId,
            @RequestParam Instant publishTime,
            @RequestParam(defaultValue = "UTC") String timezone,
            @RequestParam(defaultValue = "false") boolean recurring,
            @RequestParam(required = false) String recurringPattern,
            @RequestAttribute("userId") String userId) {

        log.info("Scheduling page: {} for publish at {} ({})", pageId, publishTime, timezone);

        PagePublishService.ScheduleResult result = publishService.schedulePublish(
                pageId, userId, publishTime, timezone, recurring, recurringPattern);

        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/pages/{pageId}/schedule")
    @Operation(summary = "Cancel scheduled publish", description = "Cancels a scheduled publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PagePublishService.CancelResult> cancelScheduledPublish(
            @PathVariable String pageId,
            @RequestAttribute("userId") String userId) {

        log.info("Cancelling scheduled publish for page: {} by user: {}", pageId, userId);

        PagePublishService.CancelResult result = publishService.cancelScheduledPublish(pageId, userId);

        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    // =========================================================================
    // Canary Deployments
    // =========================================================================

    @PostMapping("/canary")
    @Operation(summary = "Publish canary", description = "Performs canary deployment for a page")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PagePublishService.CanaryDeploymentResult> publishCanary(
            @RequestParam String pageId,
            @RequestParam int percentage,
            @RequestParam(defaultValue = "300000") long monitoringDurationMs,
            @RequestAttribute("userId") String userId) {

        log.info("Canary publishing page: {} with {}% traffic by user: {}", pageId, percentage, userId);

        PagePublishService.CanaryDeploymentResult result = publishService.publishCanary(pageId, userId, percentage, monitoringDurationMs);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/canary/{canaryId}/promote")
    @Operation(summary = "Promote canary", description = "Promotes canary deployment to full production")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagePublishService.PromotionResult> promoteCanary(
            @PathVariable String canaryId,
            @RequestAttribute("userId") String userId) {

        log.info("Promoting canary: {} by user: {}", canaryId, userId);

        PagePublishService.PromotionResult result = publishService.promoteCanary(canaryId, userId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/canary/{canaryId}/rollback")
    @Operation(summary = "Rollback canary", description = "Rolls back canary deployment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagePublishService.RollbackResult> rollbackCanary(
            @PathVariable String canaryId,
            @RequestParam String reason,
            @RequestAttribute("userId") String userId) {

        log.info("Rolling back canary: {} by user: {} reason: {}", canaryId, userId, reason);

        PagePublishService.RollbackResult result = publishService.rollbackCanary(canaryId, userId, reason);

        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Batch Publishing
    // =========================================================================

    @PostMapping("/batch")
    @Operation(summary = "Batch publish pages", description = "Publishes multiple pages in batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> batchPublishPages(
            @RequestParam List<String> pageIds,
            @RequestParam(required = false) Instant publishTime,
            @RequestAttribute("userId") String userId) {

        log.info("Batch publishing {} pages by user: {}", pageIds.size(), userId);

        PagePublishService.BatchPublishOptions options = PagePublishService.BatchPublishOptions.defaultOptions();

        String batchId = publishService.batchPublishPages(pageIds, userId, publishTime, options);

        return ResponseEntity.accepted().body(batchId);
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get batch status", description = "Gets the status of a batch publishing job")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagePublishService.BatchStatus> getBatchPublishStatus(
            @PathVariable String batchId) {

        log.debug("Getting batch status for: {}", batchId);

        PagePublishService.BatchStatus status = publishService.getBatchPublishStatus(batchId);

        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/batch/{batchId}")
    @Operation(summary = "Cancel batch publish", description = "Cancels a batch publishing job")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagePublishService.BatchCancelResult> cancelBatchPublish(
            @PathVariable String batchId,
            @RequestAttribute("userId") String userId) {

        log.info("Cancelling batch publish: {} by user: {}", batchId, userId);

        PagePublishService.BatchCancelResult result = publishService.cancelBatchPublish(batchId, userId);

        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Publishing Analytics
    // =========================================================================

    @GetMapping("/analytics")
    @Operation(summary = "Get publishing analytics", description = "Retrieves publishing analytics for workspace")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagePublishService.PublishAnalytics> getPublishAnalytics(
            @RequestParam String workspaceId,
            @RequestParam(defaultValue = "30") int days) {

        log.debug("Getting publish analytics for workspace: {} over {} days", workspaceId, days);

        PagePublishService.PublishAnalytics analytics = publishService.getPublishAnalytics(workspaceId, days);

        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/pages/{pageId}/optimal-time")
    @Operation(summary = "Predict optimal publish time", description = "Predicts optimal publish time for a page")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<List<PagePublishService.PublishTimePrediction>> predictOptimalPublishTime(
            @PathVariable String pageId,
            @RequestParam(required = false) List<String> timezones,
            @RequestParam(required = false) List<String> regions) {

        log.debug("Predicting optimal publish time for page: {}", pageId);

        PagePublishService.AudienceProfile audience = new PagePublishService.AudienceProfile(
                timezones != null ? timezones : List.of("UTC"),
                regions != null ? regions : List.of("GLOBAL"),
                0, 100, "ALL");

        List<PagePublishService.PublishTimePrediction> predictions = publishService.predictOptimalPublishTime(pageId, audience);

        return ResponseEntity.ok(predictions);
    }

    @GetMapping("/pages/{pageId}/performance")
    @Operation(summary = "Analyze publish performance", description = "Analyzes performance after publishing")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PagePublishService.PublishPerformance> analyzePublishPerformance(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "7") int daysAfterPublish) {

        log.debug("Analyzing publish performance for page: {} after {} days", pageId, daysAfterPublish);

        PagePublishService.PublishPerformance performance = publishService.analyzePublishPerformance(pageId, daysAfterPublish);

        return ResponseEntity.ok(performance);
    }

    // =========================================================================
    // Publishing Validation
    // =========================================================================

    @GetMapping("/pages/{pageId}/validate")
    @Operation(summary = "Validate publish readiness", description = "Validates if page is ready for publishing")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PagePublishService.PublishValidationReport> validatePublishReadiness(
            @PathVariable String pageId) {

        log.debug("Validating publish readiness for page: {}", pageId);

        PagePublishService.PublishValidationReport report = publishService.validatePublishReadiness(pageId);

        return ResponseEntity.ok(report);
    }

    @PostMapping("/pages/{pageId}/simulate")
    @Operation(summary = "Simulate publish", description = "Simulates publishing without executing")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PagePublishService.PublishSimulation> simulatePublish(
            @PathVariable String pageId) {

        log.debug("Simulating publish for page: {}", pageId);

        PagePublishService.PublishSimulation simulation = publishService.simulatePublish(pageId);

        return ResponseEntity.ok(simulation);
    }

    // =========================================================================
    // Webhook Management
    // =========================================================================

    @PostMapping("/webhooks")
    @Operation(summary = "Register webhook", description = "Registers a webhook for publish events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> registerWebhook(
            @RequestParam String workspaceId,
            @RequestParam String url,
            @RequestParam List<String> events,
            @RequestParam(required = false) String secret) {

        log.info("Registering webhook for workspace: {} at URL: {}", workspaceId, url);

        String webhookId = publishService.registerWebhook(workspaceId, url, events, secret);

        return ResponseEntity.ok(webhookId);
    }

    @DeleteMapping("/webhooks/{webhookId}")
    @Operation(summary = "Unregister webhook", description = "Unregisters a webhook")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unregisterWebhook(
            @PathVariable String webhookId,
            @RequestAttribute("userId") String userId) {

        log.info("Unregistering webhook: {} by user: {}", webhookId, userId);

        publishService.unregisterWebhook(webhookId, userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/webhooks/{webhookId}/history")
    @Operation(summary = "Get webhook history", description = "Retrieves webhook delivery history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagePublishService.WebhookDelivery>> getWebhookHistory(
            @PathVariable String webhookId,
            @RequestParam(defaultValue = "50") int limit) {

        log.debug("Getting webhook history for: {}", webhookId);

        List<PagePublishService.WebhookDelivery> history = publishService.getWebhookHistory(webhookId, limit);

        return ResponseEntity.ok(history);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private PagePublishService.PublishOptions buildPublishOptions(PublishPageRequest request) {
        PagePublishService.PublishOptions options = PagePublishService.PublishOptions.defaultOptions();
        options.setInvalidateCache(request.isInvalidateCache());
        options.setUpdateSitemap(request.isUpdateSitemap());
        options.setPingSearchEngines(request.isPingSearchEngines());
        options.setNotifySubscribers(request.isNotifySubscribers());
        return options;
    }
}