package com.purehome.uicore.controller;

import com.purehome.uicore.dto.request.*;
import com.purehome.uicore.dto.response.*;
import com.purehome.uicore.exception.ConflictException;
import com.purehome.uicore.model.PageLayout;
import com.purehome.uicore.service.LayoutService;
import com.purehome.uicore.service.WebSocketService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * FAANG-ULTRA LAYOUT CONTROLLER
 * ============================================================================
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/layouts")
@RequiredArgsConstructor
@Tag(name = "Layout Engine Ultra", description = "FAANG-ULTRA Layout Management with Drag-Drop, Real-time Collaboration, and Predictive Optimization")
@SecurityRequirement(name = "bearerAuth")
public class LayoutController {

    private final LayoutService layoutService;
    private final WebSocketService webSocketService;
    private final MeterRegistry meterRegistry;

    // Performance timers - initialized in @PostConstruct
    private Timer dragTimer;
    private Timer dropTimer;

    @PostConstruct
    public void init() {
        this.dragTimer = Timer.builder("layout.drag.duration")
                .description("Duration of drag operations")
                .publishPercentileHistogram(true)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.dropTimer = Timer.builder("layout.drop.duration")
                .description("Duration of drop operations")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    // =========================================================================
    // CORE DRAG-DROP OPERATIONS
    // =========================================================================

    /**
     * FAANG-ULTRA DRAG OPERATION
     */
    @PostMapping(value = "/pages/{pageId}/components/drag",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Ultra drag operation",
            description = "Initiates drag operation with predictive drop zone optimization."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Drag initiated successfully",
                    content = @Content(schema = @Schema(implementation = DragPredictionResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Component not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Concurrent edit conflict detected"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DragPredictionResponse> dragComponent(
            @Parameter(description = "Page ID containing the component", required = true)
            @PathVariable String pageId,

            @Parameter(description = "Drag operation details with cursor position", required = true)
            @Valid @RequestBody DragRequest request,

            @Parameter(description = "Current user ID from JWT", hidden = true)
            @RequestAttribute("userId") String userId,

            @Parameter(description = "Correlation ID for distributed tracing", hidden = true)
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Ultra drag initiated - Page: {}, Component: {}, User: {}, Position: ({},{})",
                pageId, request.getComponentId(), userId, request.getCursorX(), request.getCursorY());

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            DragPredictionResponse prediction = dragTimer.record(() ->
                    layoutService.predictDragDropTargets(pageId, request, userId, correlationId)
            );

            meterRegistry.counter("layout.drag.predictions",
                    "targetCount", String.valueOf(prediction.getPredictedTargets() != null ?
                            prediction.getPredictedTargets().size() : 0),
                    "confidence", String.valueOf(prediction.getConfidenceScore())
            ).increment();

            // Convert prediction targets to the format expected by DragEvent
            List<DragEvent.PredictedTargetInfo> predictedTargetInfos = prediction.getPredictedTargets() != null ?
                    prediction.getPredictedTargets().stream()
                            .map(pt -> new DragEvent.PredictedTargetInfo(
                                    pt.getTargetId(),
                                    pt.getProbability(),
                                    pt.getExpectedX(),
                                    pt.getExpectedY()
                            ))
                            .collect(Collectors.toList()) : List.of();

            // Send real-time drag updates to collaborators
            CompletableFuture.runAsync(() ->
                    webSocketService.broadcast("/topic/layouts/" + pageId + "/drag",
                            DragEvent.builder()
                                    .componentId(request.getComponentId())
                                    .userId(userId)
                                    .cursorX(request.getCursorX())
                                    .cursorY(request.getCursorY())
                                    .predictedTargets(predictedTargetInfos)
                                    .timestamp(Instant.now())
                                    .build()
                    )
            );

            sample.stop(Timer.builder("layout.drag.processing").register(meterRegistry));

            return ResponseEntity.ok(prediction);

        } catch (Exception e) {
            log.error("Drag operation failed - Page: {}, Component: {}, Error: {}",
                    pageId, request.getComponentId(), e.getMessage(), e);
            meterRegistry.counter("layout.drag.errors", "error", e.getClass().getSimpleName()).increment();
            throw e;
        }
    }

    /**
     * FAANG-ULTRA DROP OPERATION
     */
    @PostMapping(value = "/pages/{pageId}/components/drop",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Ultra drop operation",
            description = "Completes drag-drop operation with quantum resolution."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Drop completed successfully",
                    content = @Content(schema = @Schema(implementation = DropResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict detected - needs resolution"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<DropResponse> dropComponent(
            @Parameter(description = "Page ID", required = true)
            @PathVariable String pageId,

            @Parameter(description = "Drop operation details with target position", required = true)
            @Valid @RequestBody DropRequest request,

            @Parameter(description = "User ID", hidden = true)
            @RequestAttribute("userId") String userId,

            @Parameter(description = "Version vector for conflict detection", required = true)
            @RequestHeader("X-Version-Vector") String versionVector,

            @Parameter(description = "Correlation ID", hidden = true)
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Ultra drop initiated - Page: {}, Component: {}, Target: {}/{}, Vector: {}",
                pageId, request.getComponentId(), request.getTargetSectionId(),
                request.getTargetIndex(), versionVector);

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            DropResponse response = dropTimer.record(() ->
                    layoutService.executeDrop(pageId, request, userId, versionVector, correlationId)
            );

            meterRegistry.counter("layout.drops",
                    "targetSection", request.getTargetSectionId() != null ? request.getTargetSectionId() : "null",
                    "success", String.valueOf(response.isSuccess())
            ).increment();

            if (response.isSuccess()) {
                CompletableFuture.runAsync(() -> {
                    LayoutUpdateEvent event = LayoutUpdateEvent.builder()
                            .type("DROP")
                            .componentId(request.getComponentId())
                            .targetSectionId(request.getTargetSectionId())
                            .targetPosition(request.getTargetIndex())
                            .newLayout(response.getUpdatedLayout())
                            .userId(userId)
                            .timestamp(Instant.now())
                            .versionVector(response.getNewVersionVector())
                            .build();

                    webSocketService.broadcast("/topic/layouts/" + pageId + "/updates", event);
                    auditDropEvent(pageId, request, userId, response);
                });
            }

            sample.stop(Timer.builder("layout.drop.processing").register(meterRegistry));

            return ResponseEntity.ok(response);

        } catch (ConflictException e) {
            log.warn("Drop conflict detected - Page: {}, Component: {}, Conflict: {}",
                    pageId, request.getComponentId(), e.getMessage());

            // Build ConflictDetails using builder pattern
            DropResponse.ConflictDetails conflictDetails = DropResponse.ConflictDetails.builder()
                    .conflictType(e.getConflictType() != null ? e.getConflictType() : "CONCURRENT_EDIT")
                    .description(e.getMessage())
                    .serverVersionVector(e.getServerVersionVector())
                    .clientVersionVector(e.getClientVersionVector())
                    .conflictingUserId(e.getDetectedBy())
                    .conflictTimestamp(System.currentTimeMillis())
                    .autoMergePreview(null)
                    .build();

            // Convert resolution options to strings
            List<String> resolutionOptions = e.getResolutionOptions() != null ?
                    e.getResolutionOptions().stream()
                            .map(opt -> opt.getStrategy())
                            .collect(Collectors.toList()) : List.of();

            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    DropResponse.conflict(conflictDetails, resolutionOptions)
            );
        } catch (Exception e) {
            log.error("Drop operation failed - Page: {}, Component: {}, Error: {}",
                    pageId, request.getComponentId(), e.getMessage(), e);
            meterRegistry.counter("layout.drop.errors", "error", e.getClass().getSimpleName()).increment();
            throw e;
        }
    }

    /**
     * FAANG-ULTRA BATCH REORDER OPERATION
     */
    @PutMapping(value = "/pages/{pageId}/components/reorder",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Batch component reorder",
            description = "Reorders multiple components in a single atomic operation."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<BatchReorderResponse> batchReorderComponents(
            @PathVariable String pageId,
            @Valid @RequestBody BatchReorderRequest request,
            @RequestAttribute("userId") String userId,
            @RequestHeader("X-Version-Vector") String versionVector) {

        log.info("Batch reorder - Page: {}, Section: {}, Components: {}",
                pageId, request.getSectionId(), request.getComponentIds() != null ? request.getComponentIds().size() : 0);

        BatchReorderResponse response = layoutService.batchReorderComponents(
                pageId, request, userId, versionVector
        );

        if (response != null && response.isSuccess()) {
            CompletableFuture.runAsync(() -> {
                LayoutUpdateEvent event = LayoutUpdateEvent.builder()
                        .type("BATCH_REORDER")
                        .sectionId(request.getSectionId())
                        .newOrder(request.getComponentIds())
                        .newLayout(response.getUpdatedLayout())
                        .userId(userId)
                        .timestamp(Instant.now())
                        .build();

                webSocketService.broadcast("/topic/layouts/" + pageId + "/updates", event);
            });
        }

        return ResponseEntity.ok(response);
    }

    /**
     * FAANG-ULTRA COMPONENT MOVEMENT
     */
    @PutMapping(value = "/pages/{pageId}/components/move",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Move component between sections", description = "Moves component to different section")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<MoveComponentResponse> moveComponent(
            @PathVariable String pageId,
            @Valid @RequestBody MoveComponentRequest request,
            @RequestAttribute("userId") String userId,
            @RequestHeader("X-Version-Vector") String versionVector) {

        log.info("Moving component - Page: {}, Component: {}, From: {}, To: {}",
                pageId, request.getComponentId(), request.getSourceSectionId(), request.getTargetSectionId());

        MoveComponentResponse response = layoutService.moveComponent(
                pageId, request, userId, versionVector
        );

        return ResponseEntity.ok(response);
    }

    /**
     * FAANG-ULTRA LAYOUT RENDERING
     */
    @GetMapping(value = "/pages/{pageId}/render", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Render layout for device", description = "Returns optimized layout for specific device type")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<RenderResponse> renderLayout(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "desktop") String deviceType,
            @RequestParam(defaultValue = "false") boolean preview,
            @RequestHeader(value = "X-Client-Version", required = false) String clientVersion,
            @RequestHeader(value = "X-Viewport-Width", required = false) Integer viewportWidth,
            @RequestHeader(value = "X-Viewport-Height", required = false) Integer viewportHeight) {

        log.debug("Rendering layout - Page: {}, Device: {}, Preview: {}, Viewport: {}x{}",
                pageId, deviceType, preview, viewportWidth, viewportHeight);

        RenderResponse response = layoutService.renderLayout(
                pageId, deviceType, preview, clientVersion, viewportWidth, viewportHeight
        );

        return ResponseEntity.ok(response);
    }

    /**
     * FAANG-ULTRA LAYOUT VALIDATION
     */
    @PostMapping(value = "/pages/{pageId}/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validate layout integrity", description = "Performs comprehensive layout validation")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<LayoutValidationResponse> validateLayout(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "full") String validationLevel) {

        log.debug("Validating layout - Page: {}, Level: {}", pageId, validationLevel);

        LayoutValidationResponse validation = layoutService.validateLayout(pageId, validationLevel);

        HttpStatus status = validation != null && validation.isValid() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(validation);
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    private void auditDropEvent(String pageId, DropRequest request, String userId, DropResponse response) {
        log.debug("Audit: Drop event recorded - Page: {}, Component: {}, User: {}, Success: {}",
                pageId, request.getComponentId(), userId, response != null && response.isSuccess());
    }

    // =========================================================================
    // INNER CLASSES FOR EVENTS
    // =========================================================================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class DragEvent {
        private String componentId;
        private String userId;
        private Integer cursorX;
        private Integer cursorY;
        private List<PredictedTargetInfo> predictedTargets;
        private Instant timestamp;

        @lombok.Data
        @lombok.AllArgsConstructor
        @lombok.NoArgsConstructor
        public static class PredictedTargetInfo {
            private String targetId;
            private Double probability;
            private Integer expectedX;
            private Integer expectedY;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class LayoutUpdateEvent {
        private String type;
        private String componentId;
        private String targetSectionId;
        private Integer targetPosition;
        private String sectionId;
        private List<String> newOrder;
        private PageLayout newLayout;
        private String userId;
        private Instant timestamp;
        private String versionVector;
    }
}