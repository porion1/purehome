package com.purehome.uicore.controller;

import com.purehome.uicore.dto.request.RollbackVersionRequest;
import com.purehome.uicore.dto.response.VersionResponse;
import com.purehome.uicore.dto.response.VersionDiffResponse;
import com.purehome.uicore.dto.response.VersionGraphResponse;
import com.purehome.uicore.dto.response.MergeAnalysisResponse;
import com.purehome.uicore.model.PageVersion.ChangeType;
import com.purehome.uicore.service.PageVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/pages/{pageId}/versions")
@RequiredArgsConstructor
@Tag(name = "Page Version Management", description = "APIs for managing page versions, branches, and rollbacks")
@SecurityRequirement(name = "bearerAuth")
public class PageVersionController {

    private final PageVersionService versionService;

    // =========================================================================
    // Core Version Operations
    // =========================================================================

    @GetMapping
    @Operation(summary = "Get version history", description = "Retrieves paginated version history for a page")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<PageVersionService.VersionHistoryResponse> getVersionHistory(
            @PathVariable String pageId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) ChangeType changeType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        log.debug("Fetching version history for page: {}", pageId);

        PageVersionService.VersionHistoryResponse history = versionService.getVersionHistory(
                pageId, cursor, limit, changeType, startDate, endDate);

        return ResponseEntity.ok(history);
    }

    @GetMapping("/current")
    @Operation(summary = "Get current version", description = "Retrieves the current (head) version of a page")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionResponse> getCurrentVersion(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "false") boolean publishedOnly) {

        log.debug("Fetching current version for page: {} (publishedOnly: {})", pageId, publishedOnly);

        Optional<VersionResponse> version = versionService.getCurrentVersion(pageId, publishedOnly);

        return version.map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("No version found for page: " + pageId));
    }

    @GetMapping("/{versionNumber}")
    @Operation(summary = "Get version by number", description = "Retrieves a specific version by its number")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionResponse> getVersionByNumber(
            @PathVariable String pageId,
            @PathVariable Integer versionNumber) {

        log.debug("Fetching version {} for page: {}", versionNumber, pageId);

        Optional<VersionResponse> version = versionService.getVersionByNumber(pageId, versionNumber);

        return version.map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionNumber));
    }

    @GetMapping("/at-time")
    @Operation(summary = "Get version at time", description = "Time-travel query to get page state at a specific timestamp")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionResponse> getVersionAtTime(
            @PathVariable String pageId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant timestamp,
            @RequestParam(defaultValue = "UTC") String timezone) {

        log.debug("Fetching version for page: {} at time: {} ({})", pageId, timestamp, timezone);

        Optional<VersionResponse> version = versionService.getVersionAtTime(pageId, timestamp, timezone);

        return version.map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("No version found at time: " + timestamp));
    }

    // =========================================================================
    // Version Comparison & Diff
    // =========================================================================

    @GetMapping("/compare")
    @Operation(summary = "Compare versions", description = "Compares two versions and returns detailed diff")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionDiffResponse> compareVersions(
            @PathVariable String pageId,
            @RequestParam String versionId1,
            @RequestParam String versionId2,
            @RequestParam(defaultValue = "UNIFIED") PageVersionService.DiffFormat format) {

        log.debug("Comparing versions {} and {} for page: {}", versionId1, versionId2, pageId);

        VersionDiffResponse diff = versionService.compareVersions(versionId1, versionId2, format);

        return ResponseEntity.ok(diff);
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get version timeline", description = "Retrieves version timeline with milestones")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<PageVersionService.VersionTimeline> getVersionTimeline(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "30") int days) {

        log.debug("Fetching version timeline for page: {} over {} days", pageId, days);

        PageVersionService.VersionTimeline timeline = versionService.getVersionTimeline(pageId, days);

        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get change summary", description = "Retrieves summary of changes across version range")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<PageVersionService.ChangeSummary> getChangeSummary(
            @PathVariable String pageId,
            @RequestParam(required = false) Integer fromVersion,
            @RequestParam(required = false) Integer toVersion) {

        log.debug("Fetching change summary for page: {} from {} to {}", pageId, fromVersion, toVersion);

        PageVersionService.ChangeSummary summary = versionService.getChangeSummary(pageId, fromVersion, toVersion);

        return ResponseEntity.ok(summary);
    }

    // =========================================================================
    // Rollback & Recovery
    // =========================================================================

    @PostMapping("/rollback")
    @Operation(summary = "Rollback to version", description = "Rolls back page to a previous version")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<VersionResponse> rollbackToVersion(
            @PathVariable String pageId,
            @Valid @RequestBody RollbackVersionRequest request,
            @RequestAttribute("userId") String userId) {

        log.info("Rolling back page {} to version {} by user: {}", pageId, request.getTargetVersion(), userId);

        VersionResponse result = versionService.rollbackToVersion(pageId, request.getTargetVersion(), userId, request.getReason());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/restore/{versionId}")
    @Operation(summary = "Restore version", description = "Restores a deleted version")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VersionResponse> restoreVersion(
            @PathVariable String pageId,
            @PathVariable String versionId,
            @RequestAttribute("userId") String userId) {

        log.info("Restoring version {} for page {} by user: {}", versionId, pageId, userId);

        VersionResponse result = versionService.restoreVersion(versionId, userId);

        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Branch Management
    // =========================================================================

    @GetMapping("/branches")
    @Operation(summary = "List branches", description = "Lists all branches for a page")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<List<PageVersionService.BranchInfo>> listBranches(
            @PathVariable String pageId) {

        log.debug("Listing branches for page: {}", pageId);

        List<PageVersionService.BranchInfo> branches = versionService.listBranches(pageId);

        return ResponseEntity.ok(branches);
    }

    @PostMapping("/branches")
    @Operation(summary = "Create branch", description = "Creates a new branch from a specific version")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<VersionResponse> createBranch(
            @PathVariable String pageId,
            @RequestParam Integer sourceVersion,
            @RequestParam String branchName,
            @RequestAttribute("userId") String userId) {

        log.info("Creating branch '{}' from version {} for page {} by user: {}", branchName, sourceVersion, pageId, userId);

        VersionResponse result = versionService.createBranch(pageId, sourceVersion, branchName, userId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/branches/merge")
    @Operation(summary = "Merge branch", description = "Merges a source branch into target branch")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<MergeAnalysisResponse> mergeBranch(
            @PathVariable String pageId,
            @RequestParam String sourceBranch,
            @RequestParam String targetBranch,
            @RequestParam(defaultValue = "THREE_WAY") PageVersionService.MergeStrategy strategy,
            @RequestAttribute("userId") String userId) {

        log.info("Merging branch '{}' into '{}' for page {} by user: {}", sourceBranch, targetBranch, pageId, userId);

        MergeAnalysisResponse result = versionService.mergeBranch(pageId, sourceBranch, targetBranch, userId, strategy);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/branches/compare")
    @Operation(summary = "Compare branches", description = "Compares two branches and calculates divergence")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<PageVersionService.BranchComparison> compareBranches(
            @PathVariable String pageId,
            @RequestParam String branch1,
            @RequestParam String branch2) {

        log.debug("Comparing branches '{}' and '{}' for page: {}", branch1, branch2, pageId);

        PageVersionService.BranchComparison comparison = versionService.compareBranches(pageId, branch1, branch2);

        return ResponseEntity.ok(comparison);
    }

    @GetMapping("/branches/{branchName}/health")
    @Operation(summary = "Get branch health", description = "Retrieves health metrics for a branch")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PageVersionService.BranchHealthReport> getBranchHealth(
            @PathVariable String pageId,
            @PathVariable String branchName) {

        log.debug("Getting health for branch '{}' on page: {}", branchName, pageId);

        PageVersionService.BranchHealthReport health = versionService.getBranchHealth(pageId, branchName);

        return ResponseEntity.ok(health);
    }

    // =========================================================================
    // Version Graph
    // =========================================================================

    @GetMapping("/graph")
    @Operation(summary = "Get version graph", description = "Retrieves complete version graph for visualization")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionGraphResponse> getVersionGraph(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "0") int maxDepth) {

        log.debug("Fetching version graph for page: {} with max depth: {}", pageId, maxDepth);

        VersionGraphResponse graph = versionService.getVersionGraph(pageId, maxDepth);

        return ResponseEntity.ok(graph);
    }

    @GetMapping("/common-ancestor")
    @Operation(summary = "Find common ancestor", description = "Finds common ancestor between two versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionResponse> findCommonAncestor(
            @PathVariable String pageId,
            @RequestParam String versionId1,
            @RequestParam String versionId2) {

        log.debug("Finding common ancestor between versions {} and {}", versionId1, versionId2);

        Optional<VersionResponse> ancestor = versionService.findCommonAncestor(versionId1, versionId2);

        return ancestor.map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("No common ancestor found"));
    }

    @GetMapping("/influence-scores")
    @Operation(summary = "Calculate version influence", description = "Calculates PageRank influence scores for versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Double>> calculateVersionInfluence(
            @PathVariable String pageId) {

        log.debug("Calculating version influence scores for page: {}", pageId);

        Map<String, Double> scores = versionService.calculateVersionInfluence(pageId);

        return ResponseEntity.ok(scores);
    }

    // =========================================================================
    // Version Integrity
    // =========================================================================

    @GetMapping("/integrity")
    @Operation(summary = "Verify chain integrity", description = "Verifies the cryptographic integrity of the version chain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageVersionService.IntegrityReport> verifyVersionChain(
            @PathVariable String pageId) {

        log.debug("Verifying version chain integrity for page: {}", pageId);

        PageVersionService.IntegrityReport report = versionService.verifyVersionChain(pageId);

        return ResponseEntity.ok(report);
    }

    @PostMapping("/integrity/repair")
    @Operation(summary = "Repair version chain", description = "Repairs corrupted version chain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageVersionService.IntegrityReport> repairVersionChain(
            @PathVariable String pageId,
            @RequestAttribute("userId") String userId) {

        log.info("Repairing version chain for page: {} by user: {}", pageId, userId);

        PageVersionService.IntegrityReport report = versionService.repairVersionChain(pageId, userId);

        return ResponseEntity.ok(report);
    }

    // =========================================================================
    // Version Cleanup & Optimization
    // =========================================================================

    @DeleteMapping("/prune")
    @Operation(summary = "Prune old versions", description = "Prunes old versions based on retention policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageVersionService.PruneResult> pruneVersions(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "90") int retentionDays,
            @RequestAttribute("userId") String userId) {

        log.info("Pruning versions for page: {} older than {} days by user: {}", pageId, retentionDays, userId);

        PageVersionService.PruneResult result = versionService.pruneVersions(pageId, retentionDays, userId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/optimize-storage")
    @Operation(summary = "Optimize version storage", description = "Optimizes version storage with delta compression")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageVersionService.OptimizationResult> optimizeVersionStorage(
            @PathVariable String pageId) {

        log.info("Optimizing version storage for page: {}", pageId);

        PageVersionService.OptimizationResult result = versionService.optimizeVersionStorage(pageId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/archive")
    @Operation(summary = "Archive old versions", description = "Archives old versions to cold storage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageVersionService.ArchiveResult> archiveVersions(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "365") int daysBeforeArchive) {

        log.info("Archiving versions for page: {} older than {} days", pageId, daysBeforeArchive);

        PageVersionService.ArchiveResult result = versionService.archiveVersions(pageId, daysBeforeArchive);

        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Version Tags
    // =========================================================================

    @PostMapping("/{versionId}/tags/{tag}")
    @Operation(summary = "Add tag to version", description = "Adds a tag to a specific version")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Void> addVersionTag(
            @PathVariable String pageId,
            @PathVariable String versionId,
            @PathVariable String tag,
            @RequestAttribute("userId") String userId) {

        log.info("Adding tag '{}' to version {} for page {} by user: {}", tag, versionId, pageId, userId);

        versionService.addVersionTag(versionId, tag, userId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{versionId}/tags/{tag}")
    @Operation(summary = "Remove tag from version", description = "Removes a tag from a specific version")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Void> removeVersionTag(
            @PathVariable String pageId,
            @PathVariable String versionId,
            @PathVariable String tag,
            @RequestAttribute("userId") String userId) {

        log.info("Removing tag '{}' from version {} for page {} by user: {}", tag, versionId, pageId, userId);

        versionService.removeVersionTag(versionId, tag, userId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/tags")
    @Operation(summary = "Get versions by tags", description = "Retrieves versions matching specific tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<List<VersionResponse>> getVersionsByTags(
            @PathVariable String pageId,
            @RequestParam List<String> tags) {

        log.debug("Fetching versions for page {} with tags: {}", pageId, tags);

        List<VersionResponse> versions = versionService.getVersionsByTags(pageId, tags);

        return ResponseEntity.ok(versions);
    }
}