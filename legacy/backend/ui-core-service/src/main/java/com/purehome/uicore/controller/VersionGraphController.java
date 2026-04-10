package com.purehome.uicore.controller;

import com.purehome.uicore.dto.response.VersionGraphResponse;
import com.purehome.uicore.dto.response.VersionPathResponse;
import com.purehome.uicore.service.VersionGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/version-graph")
@RequiredArgsConstructor
@Tag(name = "Version Graph Analysis", description = "APIs for version graph analysis, traversal, and optimization")
@SecurityRequirement(name = "bearerAuth")
public class VersionGraphController {

    private final VersionGraphService graphService;

    // =========================================================================
    // Graph Construction & Navigation
    // =========================================================================

    @GetMapping("/pages/{pageId}")
    @Operation(summary = "Build version graph", description = "Builds complete version graph for a page")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionGraphResponse> buildVersionGraph(
            @PathVariable String pageId,
            @RequestParam(defaultValue = "true") boolean includeMetadata,
            @RequestParam(defaultValue = "0") int maxDepth) {

        log.debug("Building version graph for page: {} with max depth: {}", pageId, maxDepth);

        VersionGraphResponse graph = graphService.buildVersionGraph(pageId, includeMetadata, maxDepth);

        return ResponseEntity.ok(graph);
    }

    @GetMapping("/nodes/{nodeId}")
    @Operation(summary = "Get graph node", description = "Retrieves a specific graph node by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionGraphService.GraphNode> getGraphNode(
            @PathVariable String nodeId,
            @RequestParam(defaultValue = "false") boolean verifyIntegrity) {

        log.debug("Fetching graph node: {}", nodeId);

        return graphService.getGraphNode(nodeId, verifyIntegrity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("Graph node not found: " + nodeId));
    }

    @GetMapping("/nodes/{nodeId}/ancestors")
    @Operation(summary = "Get ancestors", description = "Retrieves ancestors of a graph node")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<List<VersionGraphService.GraphNode>> getAncestors(
            @PathVariable String nodeId) {

        log.debug("Getting ancestors for node: {}", nodeId);

        List<VersionGraphService.GraphNode> ancestors = graphService.getAncestors(nodeId);

        return ResponseEntity.ok(ancestors);
    }

    @GetMapping("/nodes/{nodeId}/descendants")
    @Operation(summary = "Get descendants", description = "Retrieves descendants of a graph node")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<List<VersionGraphService.GraphNode>> getDescendants(
            @PathVariable String nodeId,
            @RequestParam(required = false) String branchName) {

        log.debug("Getting descendants for node: {} with branch: {}", nodeId, branchName);

        List<VersionGraphService.GraphNode> descendants = graphService.getDescendants(nodeId, branchName);

        return ResponseEntity.ok(descendants);
    }

    @GetMapping("/nodes/{nodeId}/path-to-root")
    @Operation(summary = "Get path to root", description = "Retrieves the path from node to root")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionPathResponse> getPathToRoot(
            @PathVariable String nodeId) {

        log.debug("Getting path to root for node: {}", nodeId);

        VersionPathResponse path = graphService.getPathToRoot(nodeId);

        return ResponseEntity.ok(path);
    }

    // =========================================================================
    // Graph Traversal & Path Finding
    // =========================================================================

    @GetMapping("/paths/shortest")
    @Operation(summary = "Find shortest path", description = "Finds the shortest path between two nodes")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionPathResponse> findShortestPath(
            @RequestParam String sourceId,
            @RequestParam String targetId) {

        log.debug("Finding shortest path from {} to {}", sourceId, targetId);

        VersionPathResponse path = graphService.findShortestPath(sourceId, targetId);

        return ResponseEntity.ok(path);
    }

    @GetMapping("/paths/all")
    @Operation(summary = "Find all paths", description = "Finds all paths between two nodes")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<List<VersionPathResponse>> findAllPaths(
            @RequestParam String sourceId,
            @RequestParam String targetId,
            @RequestParam(defaultValue = "10") int maxPaths,
            @RequestParam(defaultValue = "100") int maxDepth) {

        log.debug("Finding all paths from {} to {} (max: {}, depth: {})", sourceId, targetId, maxPaths, maxDepth);

        List<VersionPathResponse> paths = graphService.findAllPaths(sourceId, targetId, maxPaths, maxDepth);

        return ResponseEntity.ok(paths);
    }

    @GetMapping("/lca")
    @Operation(summary = "Find lowest common ancestor", description = "Finds the lowest common ancestor of two nodes")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionGraphService.GraphNode> findLowestCommonAncestor(
            @RequestParam String nodeId1,
            @RequestParam String nodeId2) {

        log.debug("Finding LCA for {} and {}", nodeId1, nodeId2);

        return graphService.findLowestCommonAncestor(nodeId1, nodeId2)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("No common ancestor found"));
    }

    @GetMapping("/topological-order")
    @Operation(summary = "Get topological order", description = "Returns topological ordering of the version graph")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<List<VersionGraphService.GraphNode>> getTopologicalOrder(
            @RequestParam String pageId) {

        log.debug("Getting topological order for page: {}", pageId);

        List<VersionGraphService.GraphNode> order = graphService.getTopologicalOrder(pageId);

        return ResponseEntity.ok(order);
    }

    @GetMapping("/strongly-connected-components")
    @Operation(summary = "Find SCCs", description = "Finds strongly connected components in the graph")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<List<VersionGraphService.StronglyConnectedComponent>> findStronglyConnectedComponents(
            @RequestParam String pageId) {

        log.debug("Finding SCCs for page: {}", pageId);

        List<VersionGraphService.StronglyConnectedComponent> sccs = graphService.findStronglyConnectedComponents(pageId);

        return ResponseEntity.ok(sccs);
    }

    // =========================================================================
    // Merge Analysis
    // =========================================================================

    @GetMapping("/merge-base")
    @Operation(summary = "Find optimal merge base", description = "Finds optimal merge base between two branches")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<VersionGraphService.MergeBaseResult> findOptimalMergeBase(
            @RequestParam String branch1,
            @RequestParam String branch2,
            @RequestParam String pageId) {

        log.debug("Finding optimal merge base for branches {} and {} on page {}", branch1, branch2, pageId);

        VersionGraphService.MergeBaseResult result = graphService.findOptimalMergeBase(branch1, branch2, pageId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/merge-complexity")
    @Operation(summary = "Calculate merge complexity", description = "Calculates merge complexity between two branches")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<VersionGraphService.MergeComplexityScore> calculateMergeComplexity(
            @RequestParam String branch1,
            @RequestParam String branch2,
            @RequestParam String pageId) {

        log.debug("Calculating merge complexity for branches {} and {} on page {}", branch1, branch2, pageId);

        VersionGraphService.MergeComplexityScore score = graphService.calculateMergeComplexity(branch1, branch2, pageId);

        return ResponseEntity.ok(score);
    }

    @GetMapping("/merge-candidates")
    @Operation(summary = "Get merge candidates", description = "Gets merge candidates for a branch")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<List<VersionGraphService.MergeCandidate>> getMergeCandidates(
            @RequestParam String branchName,
            @RequestParam String pageId,
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("Getting merge candidates for branch {} on page {}", branchName, pageId);

        List<VersionGraphService.MergeCandidate> candidates = graphService.getMergeCandidates(branchName, pageId, limit);

        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/simulate-merge")
    @Operation(summary = "Simulate merge", description = "Simulates merge without executing")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<VersionGraphService.MergeSimulation> simulateMerge(
            @RequestParam String sourceBranch,
            @RequestParam String targetBranch,
            @RequestParam String pageId) {

        log.debug("Simulating merge from {} to {} on page {}", sourceBranch, targetBranch, pageId);

        VersionGraphService.MergeSimulation simulation = graphService.simulateMerge(sourceBranch, targetBranch, pageId);

        return ResponseEntity.ok(simulation);
    }

    // =========================================================================
    // Graph Analytics
    // =========================================================================

    @GetMapping("/influence-scores")
    @Operation(summary = "Calculate influence scores", description = "Calculates PageRank influence scores")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Double>> calculateInfluenceScores(
            @RequestParam String pageId,
            @RequestParam(defaultValue = "0.85") double dampingFactor,
            @RequestParam(defaultValue = "20") int iterations) {

        log.debug("Calculating influence scores for page: {} (damping: {}, iterations: {})", pageId, dampingFactor, iterations);

        Map<String, Double> scores = graphService.calculateInfluenceScores(pageId, dampingFactor, iterations);

        return ResponseEntity.ok(scores);
    }

    @GetMapping("/personalized-influence")
    @Operation(summary = "Calculate personalized influence", description = "Calculates personalized influence scores for a user")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<Map<String, Double>> calculatePersonalizedInfluence(
            @RequestParam String pageId,
            @RequestParam String userId) {

        log.debug("Calculating personalized influence for page: {} user: {}", pageId, userId);

        Map<String, Double> scores = graphService.calculatePersonalizedInfluence(pageId, userId);

        return ResponseEntity.ok(scores);
    }

    @GetMapping("/centrality")
    @Operation(summary = "Calculate graph centrality", description = "Calculates graph centrality metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<VersionGraphService.GraphCentrality> calculateGraphCentrality(
            @RequestParam String pageId) {

        log.debug("Calculating graph centrality for page: {}", pageId);

        VersionGraphService.GraphCentrality centrality = graphService.calculateGraphCentrality(pageId);

        return ResponseEntity.ok(centrality);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get graph statistics", description = "Retrieves comprehensive graph statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<VersionGraphService.GraphStatistics> getGraphStatistics(
            @RequestParam String pageId) {

        log.debug("Getting graph statistics for page: {}", pageId);

        VersionGraphService.GraphStatistics stats = graphService.getGraphStatistics(pageId);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/evolution")
    @Operation(summary = "Analyze graph evolution", description = "Analyzes graph evolution over time")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<VersionGraphService.GraphEvolution> analyzeGraphEvolution(
            @RequestParam String pageId,
            @RequestParam(defaultValue = "7") int intervalDays) {

        log.debug("Analyzing graph evolution for page: {} over {} day intervals", pageId, intervalDays);

        VersionGraphService.GraphEvolution evolution = graphService.analyzeGraphEvolution(pageId, intervalDays);

        return ResponseEntity.ok(evolution);
    }

    // =========================================================================
    // Graph Integrity & Optimization
    // =========================================================================

    @GetMapping("/integrity")
    @Operation(summary = "Verify graph integrity", description = "Verifies cryptographic integrity of the graph")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VersionGraphService.GraphIntegrityReport> verifyGraphIntegrity(
            @RequestParam String pageId) {

        log.debug("Verifying graph integrity for page: {}", pageId);

        VersionGraphService.GraphIntegrityReport report = graphService.verifyGraphIntegrity(pageId);

        return ResponseEntity.ok(report);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate graph structure", description = "Validates graph structure for anomalies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VersionGraphService.GraphValidationResult> validateGraphStructure(
            @RequestParam String pageId) {

        log.debug("Validating graph structure for page: {}", pageId);

        VersionGraphService.GraphValidationResult result = graphService.validateGraphStructure(pageId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/repair")
    @Operation(summary = "Repair graph", description = "Repairs corrupted graph")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VersionGraphService.GraphRepairResult> repairGraph(
            @RequestParam String pageId,
            @RequestAttribute("userId") String userId) {

        log.info("Repairing graph for page: {} by user: {}", pageId, userId);

        VersionGraphService.GraphRepairResult result = graphService.repairGraph(pageId, userId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/anomalies")
    @Operation(summary = "Detect graph anomalies", description = "Detects anomalies in the graph structure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VersionGraphService.GraphAnomalyReport> detectGraphAnomalies(
            @RequestParam String pageId) {

        log.debug("Detecting graph anomalies for page: {}", pageId);

        VersionGraphService.GraphAnomalyReport report = graphService.detectGraphAnomalies(pageId);

        return ResponseEntity.ok(report);
    }

    @PostMapping("/optimize")
    @Operation(summary = "Optimize graph", description = "Optimizes graph for storage and performance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VersionGraphService.GraphOptimizationResult> optimizeGraph(
            @RequestParam String pageId,
            @RequestParam(defaultValue = "false") boolean dryRun) {

        log.info("Optimizing graph for page: {} (dryRun: {})", pageId, dryRun);

        VersionGraphService.GraphOptimizationResult result = graphService.optimizeGraph(pageId, dryRun);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/compress-chains")
    @Operation(summary = "Compress linear chains", description = "Compresses linear chains in the graph")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VersionGraphService.ChainCompressionResult> compressLinearChains(
            @RequestParam String pageId,
            @RequestParam(defaultValue = "5") int minChainLength) {

        log.info("Compressing linear chains for page: {} (min length: {})", pageId, minChainLength);

        VersionGraphService.ChainCompressionResult result = graphService.compressLinearChains(pageId, minChainLength);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/prune-orphaned")
    @Operation(summary = "Prune orphaned nodes", description = "Prunes orphaned nodes from the graph")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VersionGraphService.PruneResult> pruneOrphanedNodes(
            @RequestParam String pageId,
            @RequestAttribute("userId") String userId) {

        log.info("Pruning orphaned nodes for page: {} by user: {}", pageId, userId);

        VersionGraphService.PruneResult result = graphService.pruneOrphanedNodes(pageId, userId);

        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Graph Snapshots & Comparison
    // =========================================================================

    @PostMapping("/snapshots")
    @Operation(summary = "Create graph snapshot", description = "Creates a snapshot of the current graph state")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createGraphSnapshot(
            @RequestParam String pageId,
            @RequestParam String label) {

        log.info("Creating graph snapshot for page: {} with label: {}", pageId, label);

        String snapshotId = graphService.createGraphSnapshot(pageId, label);

        return ResponseEntity.ok(snapshotId);
    }

    @GetMapping("/snapshots/compare")
    @Operation(summary = "Compare snapshots", description = "Compares two graph snapshots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VersionGraphService.GraphComparison> compareSnapshots(
            @RequestParam String snapshotId1,
            @RequestParam String snapshotId2) {

        log.debug("Comparing snapshots {} and {}", snapshotId1, snapshotId2);

        VersionGraphService.GraphComparison comparison = graphService.compareSnapshots(snapshotId1, snapshotId2);

        return ResponseEntity.ok(comparison);
    }

    // =========================================================================
    // Graph Visualization & Export
    // =========================================================================

    @GetMapping("/export")
    @Operation(summary = "Export graph", description = "Exports graph in specified format")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<String> exportGraph(
            @RequestParam String pageId,
            @RequestParam(defaultValue = "DOT") String format,
            @RequestParam(defaultValue = "true") boolean includeLabels) {

        log.debug("Exporting graph for page: {} in format: {}", pageId, format);

        String export = graphService.exportGraph(pageId, format, includeLabels);

        return ResponseEntity.ok(export);
    }

    @GetMapping("/layout")
    @Operation(summary = "Generate graph layout", description = "Generates layout coordinates for visualization")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionGraphService.GraphLayout> generateLayout(
            @RequestParam String pageId,
            @RequestParam(defaultValue = "1200") int width,
            @RequestParam(defaultValue = "800") int height) {

        log.debug("Generating layout for page: {} ({}x{})", pageId, width, height);

        VersionGraphService.GraphLayout layout = graphService.generateLayout(pageId, width, height);

        return ResponseEntity.ok(layout);
    }

    @GetMapping("/subgraph")
    @Operation(summary = "Get subgraph", description = "Gets subgraph centered around a node")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<VersionGraphResponse> getSubgraph(
            @RequestParam String nodeId,
            @RequestParam(defaultValue = "2") int radius,
            @RequestParam(defaultValue = "100") int maxNodes) {

        log.debug("Getting subgraph for node: {} with radius: {}", nodeId, radius);

        VersionGraphResponse subgraph = graphService.getSubgraph(nodeId, radius, maxNodes);

        return ResponseEntity.ok(subgraph);
    }
}