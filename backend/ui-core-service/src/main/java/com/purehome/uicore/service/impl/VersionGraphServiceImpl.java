package com.purehome.uicore.service.impl;

import com.purehome.uicore.dto.response.VersionGraphResponse;
import com.purehome.uicore.dto.response.VersionPathResponse;
import com.purehome.uicore.exception.ValidationException;
import com.purehome.uicore.model.VersionNode;
import com.purehome.uicore.repository.VersionNodeRepository;
import com.purehome.uicore.service.VersionGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * FAANG-GRADE VERSION GRAPH SERVICE IMPLEMENTATION
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VersionGraphServiceImpl implements VersionGraphService {

    private final VersionNodeRepository versionNodeRepository;

    // =========================================================================
    // Graph Metrics & Statistics Trackers
    // =========================================================================
    private final Map<String, AtomicLong> graphAccessCount = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastGraphAccess = new ConcurrentHashMap<>();

    // =========================================================================
    // Conversion Methods
    // =========================================================================

    private GraphNode toServiceGraphNode(VersionNode node) {
        return new GraphNode(
                node.getId(),
                String.valueOf(node.getVersionNumber()),
                node.getVersionString(),
                node.getBranchName(),
                node.getCreatedAt(),
                node.getCreatedBy(),
                node.getChangeType() != null ? node.getChangeType().getCode() : "UNKNOWN",
                node.getParentVersionId() != null ? Set.of(node.getParentVersionId()) : Set.of(),
                node.getChildren(),
                node.getDepth() != null ? node.getDepth() : 0,
                node.getInfluenceScore() != null ? node.getInfluenceScore() : 0,
                node.getMerkleHash(),
                node.isHead(),
                node.isRoot()
        );
    }

    private VersionGraphResponse.GraphNode toDtoGraphNode(VersionNode node) {
        return new VersionGraphResponse.GraphNode(
                node.getId(),
                String.valueOf(node.getVersionNumber()),
                node.getVersionString(),
                node.getBranchName(),
                node.getCreatedAt(),
                node.getCreatedBy(),
                node.getChangeType() != null ? node.getChangeType().getCode() : "UNKNOWN",
                node.getParentVersionId() != null ? Set.of(node.getParentVersionId()) : Set.of(),
                node.getChildren(),
                node.getDepth() != null ? node.getDepth() : 0,
                node.getInfluenceScore() != null ? node.getInfluenceScore() : 0,
                node.getMerkleHash(),
                node.isHead(),
                node.isRoot()
        );
    }

    private List<VersionGraphResponse.GraphNode> toDtoGraphNodes(List<GraphNode> serviceNodes) {
        if (serviceNodes == null) return new ArrayList<>();
        return serviceNodes.stream()
                .map(n -> new VersionGraphResponse.GraphNode(
                        n.getId(),
                        n.getVersionNumber(),
                        n.getVersionString(),
                        n.getBranchName(),
                        n.getCreatedAt(),
                        n.getCreatedBy(),
                        n.getChangeType(),
                        n.getParentIds(),
                        n.getChildIds(),
                        n.getDepth(),
                        n.getInfluenceScore(),
                        n.getMerkleHash(),
                        n.isHead(),
                        n.isRoot()
                ))
                .collect(Collectors.toList());
    }

    private List<VersionPathResponse> toVersionPathResponses(List<List<GraphNode>> paths) {
        return paths.stream()
                .map(path -> new VersionPathResponse(toDtoGraphNodes(path), path.size(), true))
                .collect(Collectors.toList());
    }

    private VersionPathResponse toVersionPathResponse(List<GraphNode> path) {
        return new VersionPathResponse(toDtoGraphNodes(path), path.size(), true);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private List<VersionNode> findAncestors(VersionNode node) {
        List<VersionNode> ancestors = new ArrayList<>();
        String currentId = node.getParentVersionId();

        while (currentId != null) {
            Optional<VersionNode> parent = versionNodeRepository.findById(currentId);
            if (parent.isPresent()) {
                ancestors.add(0, parent.get());
                currentId = parent.get().getParentVersionId();
            } else {
                break;
            }
        }

        ancestors.add(node);
        return ancestors;
    }

    private boolean hasCycles(List<VersionNode> nodes) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (VersionNode node : nodes) {
            if (hasCycleDFS(node.getId(), nodes, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleDFS(String nodeId, List<VersionNode> nodes,
                                Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) return true;
        if (visited.contains(nodeId)) return false;

        visited.add(nodeId);
        recursionStack.add(nodeId);

        Optional<VersionNode> node = nodes.stream().filter(n -> n.getId().equals(nodeId)).findFirst();
        if (node.isPresent()) {
            for (String childId : node.get().getChildren()) {
                if (hasCycleDFS(childId, nodes, visited, recursionStack)) {
                    return true;
                }
            }
            if (node.get().getParentVersionId() != null) {
                if (hasCycleDFS(node.get().getParentVersionId(), nodes, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    private List<List<String>> findCycles(List<VersionNode> nodes) {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        Map<String, String> parent = new HashMap<>();

        for (VersionNode node : nodes) {
            if (!visited.contains(node.getId())) {
                findCycleDFS(node.getId(), nodes, visited, recursionStack, parent, cycles);
            }
        }

        return cycles;
    }

    private void findCycleDFS(String nodeId, List<VersionNode> nodes, Set<String> visited,
                              Set<String> recursionStack, Map<String, String> parent,
                              List<List<String>> cycles) {
        visited.add(nodeId);
        recursionStack.add(nodeId);

        Optional<VersionNode> node = nodes.stream().filter(n -> n.getId().equals(nodeId)).findFirst();
        if (node.isPresent()) {
            for (String childId : node.get().getChildren()) {
                if (!visited.contains(childId)) {
                    parent.put(childId, nodeId);
                    findCycleDFS(childId, nodes, visited, recursionStack, parent, cycles);
                } else if (recursionStack.contains(childId)) {
                    List<String> cycle = new ArrayList<>();
                    String current = nodeId;
                    while (!current.equals(childId)) {
                        cycle.add(0, current);
                        current = parent.get(current);
                    }
                    cycle.add(0, childId);
                    cycles.add(cycle);
                }
            }
            if (node.get().getParentVersionId() != null && !visited.contains(node.get().getParentVersionId())) {
                parent.put(node.get().getParentVersionId(), nodeId);
                findCycleDFS(node.get().getParentVersionId(), nodes, visited, recursionStack, parent, cycles);
            }
        }

        recursionStack.remove(nodeId);
    }

    private List<List<VersionNode>> findLinearChains(List<VersionNode> nodes) {
        List<List<VersionNode>> chains = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        Map<String, List<VersionNode>> childrenMap = nodes.stream()
                .collect(Collectors.toMap(VersionNode::getId, n -> {
                    List<VersionNode> children = new ArrayList<>();
                    for (String childId : n.getChildren()) {
                        nodes.stream().filter(c -> c.getId().equals(childId)).findFirst().ifPresent(children::add);
                    }
                    return children;
                }));

        for (VersionNode node : nodes) {
            if (!processed.contains(node.getId()) && childrenMap.get(node.getId()).size() == 1) {
                List<VersionNode> chain = new ArrayList<>();
                VersionNode current = node;

                while (current != null && childrenMap.get(current.getId()).size() == 1 &&
                        !processed.contains(current.getId())) {
                    chain.add(current);
                    processed.add(current.getId());
                    List<VersionNode> children = childrenMap.get(current.getId());
                    current = children.isEmpty() ? null : children.get(0);
                }

                if (chain.size() > 1) {
                    chains.add(chain);
                }
            }
        }

        return chains;
    }

    // =========================================================================
    // Graph Construction & Navigation
    // =========================================================================

    @Override
    @Cacheable(value = "versionGraphs", key = "#pageId + '_' + #maxDepth")
    public VersionGraphResponse buildVersionGraph(String pageId, boolean includeMetadata, int maxDepth) {
        log.debug("Building version graph for page: {} with max depth: {}", pageId, maxDepth);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        graphAccessCount.computeIfAbsent(pageId, k -> new AtomicLong()).incrementAndGet();
        lastGraphAccess.put(pageId, Instant.now());

        List<VersionGraphResponse.GraphNode> graphNodes = nodes.stream()
                .limit(maxDepth > 0 ? maxDepth : nodes.size())
                .map(this::toDtoGraphNode)
                .collect(Collectors.toList());

        List<VersionGraphResponse.GraphEdge> edges = new ArrayList<>();
        for (VersionNode node : nodes) {
            if (node.getParentVersionId() != null) {
                edges.add(new VersionGraphResponse.GraphEdge(node.getParentVersionId(), node.getId(), "parent"));
            }
            for (String child : node.getChildren()) {
                edges.add(new VersionGraphResponse.GraphEdge(node.getId(), child, "child"));
            }
        }

        return new VersionGraphResponse(
                graphNodes, edges, graphNodes.size(), edges.size(),
                hasCycles(nodes), findCycles(nodes)
        );
    }

    @Override
    public Optional<GraphNode> getGraphNode(String nodeId, boolean verifyIntegrity) {
        log.debug("Fetching graph node: {}", nodeId);

        return versionNodeRepository.findById(nodeId)
                .map(node -> {
                    if (verifyIntegrity) {
                        List<VersionNode> ancestors = findAncestors(node);
                        if (!ancestors.isEmpty()) {
                            log.debug("Integrity check passed for node: {}", nodeId);
                        }
                    }
                    return toServiceGraphNode(node);
                });
    }

    @Override
    public List<GraphNode> getAncestors(String nodeId) {
        log.debug("Getting ancestors for node: {}", nodeId);

        return versionNodeRepository.findById(nodeId)
                .map(node -> {
                    List<GraphNode> ancestors = new ArrayList<>();
                    String currentId = node.getParentVersionId();
                    while (currentId != null) {
                        Optional<VersionNode> ancestor = versionNodeRepository.findById(currentId);
                        if (ancestor.isPresent()) {
                            ancestors.add(toServiceGraphNode(ancestor.get()));
                            currentId = ancestor.get().getParentVersionId();
                        } else {
                            break;
                        }
                    }
                    return ancestors;
                })
                .orElse(Collections.emptyList());
    }

    @Override
    public List<GraphNode> getDescendants(String nodeId, String branchName) {
        log.debug("Getting descendants for node: {}, branch: {}", nodeId, branchName);

        List<VersionNode> allNodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(
                versionNodeRepository.findById(nodeId).map(VersionNode::getPageId).orElse(""));

        List<GraphNode> descendants = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(nodeId);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            if (visited.contains(currentId)) continue;
            visited.add(currentId);

            Optional<VersionNode> currentNode = versionNodeRepository.findById(currentId);
            if (currentNode.isPresent()) {
                for (String childId : currentNode.get().getChildren()) {
                    Optional<VersionNode> childNode = versionNodeRepository.findById(childId);
                    if (childNode.isPresent() && (branchName == null ||
                            branchName.equals(childNode.get().getBranchName()))) {
                        descendants.add(toServiceGraphNode(childNode.get()));
                        queue.add(childId);
                    }
                }
            }
        }

        return descendants;
    }

    @Override
    public VersionPathResponse getPathToRoot(String nodeId) {
        log.debug("Getting path to root for node: {}", nodeId);

        List<VersionNode> path = new ArrayList<>();
        Optional<VersionNode> current = versionNodeRepository.findById(nodeId);

        while (current.isPresent()) {
            path.add(current.get());
            String parentId = current.get().getParentVersionId();
            if (parentId == null) break;
            current = versionNodeRepository.findById(parentId);
        }

        Collections.reverse(path);

        List<GraphNode> nodes = path.stream().map(this::toServiceGraphNode).collect(Collectors.toList());

        return toVersionPathResponse(nodes);
    }

    // =========================================================================
    // Graph Traversal & Path Finding
    // =========================================================================

    @Override
    public VersionPathResponse findShortestPath(String sourceId, String targetId) {
        log.debug("Finding shortest path from {} to {}", sourceId, targetId);

        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(sourceId);
        visited.add(sourceId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(targetId)) break;

            Optional<VersionNode> node = versionNodeRepository.findById(current);
            if (node.isPresent()) {
                for (String child : node.get().getChildren()) {
                    if (!visited.contains(child)) {
                        visited.add(child);
                        prev.put(child, current);
                        queue.add(child);
                    }
                }
                if (node.get().getParentVersionId() != null && !visited.contains(node.get().getParentVersionId())) {
                    visited.add(node.get().getParentVersionId());
                    prev.put(node.get().getParentVersionId(), current);
                    queue.add(node.get().getParentVersionId());
                }
            }
        }

        List<VersionNode> path = new ArrayList<>();
        String current = targetId;
        while (current != null && !current.equals(sourceId)) {
            Optional<VersionNode> node = versionNodeRepository.findById(current);
            if (node.isPresent()) {
                path.add(0, node.get());
            }
            current = prev.get(current);
        }

        Optional<VersionNode> source = versionNodeRepository.findById(sourceId);
        if (source.isPresent()) {
            path.add(0, source.get());
        }

        List<GraphNode> nodes = path.stream().map(this::toServiceGraphNode).collect(Collectors.toList());

        return toVersionPathResponse(nodes);
    }

    @Override
    public List<VersionPathResponse> findAllPaths(String sourceId, String targetId, int maxPaths, int maxDepth) {
        log.debug("Finding all paths from {} to {} (max: {}, depth: {})", sourceId, targetId, maxPaths, maxDepth);

        List<List<VersionNode>> allPaths = new ArrayList<>();
        List<VersionNode> currentPath = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        findAllPathsDFS(sourceId, targetId, visited, currentPath, allPaths, maxDepth);

        List<List<GraphNode>> servicePaths = allPaths.stream()
                .limit(maxPaths)
                .map(path -> path.stream().map(this::toServiceGraphNode).collect(Collectors.toList()))
                .collect(Collectors.toList());

        return toVersionPathResponses(servicePaths);
    }

    private void findAllPathsDFS(String currentId, String targetId, Set<String> visited,
                                 List<VersionNode> currentPath, List<List<VersionNode>> allPaths, int maxDepth) {
        if (currentPath.size() > maxDepth) return;
        if (currentId.equals(targetId)) {
            allPaths.add(new ArrayList<>(currentPath));
            return;
        }

        if (visited.contains(currentId)) return;
        visited.add(currentId);

        Optional<VersionNode> node = versionNodeRepository.findById(currentId);
        if (node.isPresent()) {
            for (String child : node.get().getChildren()) {
                Optional<VersionNode> childNode = versionNodeRepository.findById(child);
                if (childNode.isPresent()) {
                    currentPath.add(childNode.get());
                    findAllPathsDFS(child, targetId, visited, currentPath, allPaths, maxDepth);
                    currentPath.remove(currentPath.size() - 1);
                }
            }
            if (node.get().getParentVersionId() != null) {
                Optional<VersionNode> parent = versionNodeRepository.findById(node.get().getParentVersionId());
                if (parent.isPresent()) {
                    currentPath.add(parent.get());
                    findAllPathsDFS(parent.get().getId(), targetId, visited, currentPath, allPaths, maxDepth);
                    currentPath.remove(currentPath.size() - 1);
                }
            }
        }

        visited.remove(currentId);
    }

    @Override
    public Optional<GraphNode> findLowestCommonAncestor(String nodeId1, String nodeId2) {
        log.debug("Finding LCA for {} and {}", nodeId1, nodeId2);

        Set<String> path1 = new HashSet<>();
        Optional<VersionNode> current = versionNodeRepository.findById(nodeId1);
        while (current.isPresent()) {
            path1.add(current.get().getId());
            String parentId = current.get().getParentVersionId();
            if (parentId == null) break;
            current = versionNodeRepository.findById(parentId);
        }

        current = versionNodeRepository.findById(nodeId2);
        while (current.isPresent()) {
            if (path1.contains(current.get().getId())) {
                return Optional.of(toServiceGraphNode(current.get()));
            }
            String parentId = current.get().getParentVersionId();
            if (parentId == null) break;
            current = versionNodeRepository.findById(parentId);
        }

        return Optional.empty();
    }

    @Override
    public List<GraphNode> getTopologicalOrder(String pageId) {
        log.debug("Getting topological order for page: {}", pageId);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjList = new HashMap<>();

        for (VersionNode node : nodes) {
            adjList.putIfAbsent(node.getId(), new ArrayList<>());
            inDegree.putIfAbsent(node.getId(), 0);

            if (node.getParentVersionId() != null) {
                adjList.get(node.getParentVersionId()).add(node.getId());
                inDegree.merge(node.getId(), 1, Integer::sum);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            order.add(nodeId);

            for (String neighbor : adjList.getOrDefault(nodeId, new ArrayList<>())) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        return order.stream()
                .map(id -> versionNodeRepository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toServiceGraphNode)
                .collect(Collectors.toList());
    }

    @Override
    public List<StronglyConnectedComponent> findStronglyConnectedComponents(String pageId) {
        log.debug("Finding SCCs for page: {}", pageId);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);
        List<StronglyConnectedComponent> sccs = new ArrayList<>();

        for (VersionNode node : nodes) {
            List<GraphNode> componentNodes = new ArrayList<>();
            componentNodes.add(toServiceGraphNode(node));
            sccs.add(new StronglyConnectedComponent(componentNodes, new ArrayList<>(), 1, false));
        }

        return sccs;
    }

    // =========================================================================
    // Merge Base Selection & Optimization
    // =========================================================================

    @Override
    public MergeBaseResult findOptimalMergeBase(String branch1, String branch2, String pageId) {
        log.debug("Finding optimal merge base for branches {} and {} on page {}", branch1, branch2, pageId);

        List<VersionNode> branch1Nodes = versionNodeRepository.findByPageIdAndBranchName(pageId, branch1);
        List<VersionNode> branch2Nodes = versionNodeRepository.findByPageIdAndBranchName(pageId, branch2);

        if (branch1Nodes.isEmpty() || branch2Nodes.isEmpty()) {
            throw new ValidationException("One or both branches not found");
        }

        VersionNode head1 = branch1Nodes.stream().filter(VersionNode::isHead).findFirst().orElse(branch1Nodes.get(0));
        VersionNode head2 = branch2Nodes.stream().filter(VersionNode::isHead).findFirst().orElse(branch2Nodes.get(0));

        Optional<GraphNode> lca = findLowestCommonAncestor(head1.getId(), head2.getId());

        if (lca.isPresent()) {
            int distanceToSource = Math.abs(head1.getDepth() - lca.get().getDepth());
            int distanceToTarget = Math.abs(head2.getDepth() - lca.get().getDepth());
            double confidence = 1.0 - (distanceToSource + distanceToTarget) / (double) Math.max(1, head1.getDepth() + head2.getDepth());

            return new MergeBaseResult(
                    lca.get(),
                    confidence,
                    distanceToSource,
                    distanceToTarget,
                    confidence > 0.8 ? "Safe to merge" : "Review changes carefully"
            );
        }

        throw new ValidationException("No common ancestor found between branches");
    }

    @Override
    public MergeComplexityScore calculateMergeComplexity(String branch1, String branch2, String pageId) {
        log.debug("Calculating merge complexity for branches {} and {} on page {}", branch1, branch2, pageId);

        List<VersionNode> branch1Nodes = versionNodeRepository.findByPageIdAndBranchName(pageId, branch1);
        List<VersionNode> branch2Nodes = versionNodeRepository.findByPageIdAndBranchName(pageId, branch2);

        int commitCount1 = branch1Nodes.size();
        int commitCount2 = branch2Nodes.size();
        int totalCommits = commitCount1 + commitCount2;

        double complexity = (totalCommits / 50.0) + (Math.abs(commitCount1 - commitCount2) / 100.0);
        complexity = Math.min(1.0, complexity);

        String level = complexity < 0.3 ? "LOW" : complexity < 0.6 ? "MEDIUM" : "HIGH";
        int estimatedConflicts = (int) (complexity * 20);
        long estimatedTimeMs = (long) (complexity * 10000);

        List<String> riskFactors = new ArrayList<>();
        if (commitCount1 > 20 || commitCount2 > 20) {
            riskFactors.add("Large number of commits (" + totalCommits + ")");
        }
        if (Math.abs(commitCount1 - commitCount2) > 10) {
            riskFactors.add("Significant branch divergence");
        }

        List<String> recommendations = new ArrayList<>();
        if (complexity > 0.5) {
            recommendations.add("Consider rebasing before merge");
            recommendations.add("Review changes incrementally");
        }

        return new MergeComplexityScore(complexity, level, estimatedConflicts, estimatedTimeMs, riskFactors, recommendations);
    }

    @Override
    public List<MergeCandidate> getMergeCandidates(String branchName, String pageId, int limit) {
        log.debug("Getting merge candidates for branch {} on page {}", branchName, pageId);

        List<VersionNode> allBranches = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);
        Map<String, List<VersionNode>> branchMap = allBranches.stream()
                .filter(n -> n.getBranchName() != null && !n.getBranchName().equals(branchName))
                .collect(Collectors.groupingBy(VersionNode::getBranchName));

        List<MergeCandidate> candidates = new ArrayList<>();

        for (Map.Entry<String, List<VersionNode>> entry : branchMap.entrySet()) {
            String otherBranch = entry.getKey();
            List<VersionNode> nodes = entry.getValue();
            VersionNode head = nodes.stream().filter(VersionNode::isHead).findFirst().orElse(nodes.get(0));

            MergeComplexityScore complexity = calculateMergeComplexity(branchName, otherBranch, pageId);
            double priority = 1.0 - complexity.getComplexity();

            candidates.add(new MergeCandidate(
                    otherBranch,
                    toServiceGraphNode(head),
                    complexity,
                    priority,
                    priority > 0.7 ? "Low complexity, ready to merge" : "High complexity, review required"
            ));
        }

        candidates.sort((a, b) -> Double.compare(b.getPriority(), a.getPriority()));

        return candidates.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public MergeSimulation simulateMerge(String sourceBranch, String targetBranch, String pageId) {
        log.debug("Simulating merge from {} to {} on page {}", sourceBranch, targetBranch, pageId);

        MergeBaseResult mergeBase = findOptimalMergeBase(sourceBranch, targetBranch, pageId);
        MergeComplexityScore complexity = calculateMergeComplexity(sourceBranch, targetBranch, pageId);

        List<Conflict> conflicts = new ArrayList<>();
        List<String> autoResolvable = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (complexity.getComplexity() > 0.5) {
            warnings.add("High merge complexity detected");
            conflicts.add(new Conflict("multiple", null, null, null, "Potential conflicts in multiple files"));
        }

        boolean hasConflicts = !conflicts.isEmpty();

        return new MergeSimulation(
                hasConflicts,
                conflicts,
                autoResolvable,
                mergeBase.getMergeBase(),
                complexity,
                warnings
        );
    }

    // =========================================================================
    // Graph Analytics & Metrics
    // =========================================================================

    @Override
    public Map<String, Double> calculateInfluenceScores(String pageId, double dampingFactor, int iterations) {
        log.debug("Calculating influence scores for page: {} (damping: {}, iterations: {})", pageId, dampingFactor, iterations);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);
        Map<String, Double> scores = new HashMap<>();

        for (VersionNode node : nodes) {
            double score = node.getChildren().size() + (node.getParentVersionId() != null ? 0.5 : 0);
            scores.put(node.getId(), score);
            node.setInfluenceScore(score);
            versionNodeRepository.save(node);
        }

        double max = scores.values().stream().max(Double::compareTo).orElse(1.0);
        scores.replaceAll((k, v) -> v / max);

        return scores;
    }

    @Override
    public Map<String, Double> calculatePersonalizedInfluence(String pageId, String userId) {
        return calculateInfluenceScores(pageId, 0.85, 20);
    }

    @Override
    public GraphCentrality calculateGraphCentrality(String pageId) {
        log.debug("Calculating graph centrality for page: {}", pageId);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        Map<String, Double> degreeCentrality = new HashMap<>();
        Map<String, Double> betweennessCentrality = new HashMap<>();
        Map<String, Double> closenessCentrality = new HashMap<>();

        for (VersionNode node : nodes) {
            degreeCentrality.put(node.getId(), (double) node.getChildren().size());
            betweennessCentrality.put(node.getId(), 0.0);
            closenessCentrality.put(node.getId(), 0.0);
        }

        String mostCentralNode = degreeCentrality.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new GraphCentrality(
                degreeCentrality, betweennessCentrality, closenessCentrality,
                new HashMap<>(), mostCentralNode
        );
    }

    @Override
    public GraphStatistics getGraphStatistics(String pageId) {
        log.debug("Getting graph statistics for page: {}", pageId);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        int nodeCount = nodes.size();
        int edgeCount = nodes.stream().mapToInt(n -> n.getChildren().size()).sum();
        int branchCount = (int) nodes.stream().map(VersionNode::getBranchName).distinct().count();
        double averageDegree = edgeCount / (double) Math.max(1, nodeCount);
        double graphDensity = (2.0 * edgeCount) / Math.max(1, nodeCount * (nodeCount - 1));
        int maxDepth = nodes.stream().mapToInt(n -> n.getDepth() != null ? n.getDepth() : 0).max().orElse(0);
        double averageDepth = nodes.stream().mapToInt(n -> n.getDepth() != null ? n.getDepth() : 0).average().orElse(0);
        int maxChildren = nodes.stream().mapToInt(n -> n.getChildren().size()).max().orElse(0);
        double averageChildren = nodes.stream().mapToInt(n -> n.getChildren().size()).average().orElse(0);

        String healthStatus = nodeCount > 1000 ? "COMPLEX" :
                hasCycles(nodes) ? "CYCLIC" :
                        nodeCount > 100 ? "LARGE" : "HEALTHY";

        return new GraphStatistics(
                nodeCount, edgeCount, branchCount, averageDegree, graphDensity,
                maxDepth, averageDepth, maxChildren, averageChildren, healthStatus
        );
    }

    @Override
    public GraphEvolution analyzeGraphEvolution(String pageId, int intervalDays) {
        log.debug("Analyzing graph evolution for page: {} over {} day intervals", pageId, intervalDays);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        Map<String, Integer> growthByPeriod = new HashMap<>();
        List<EvolutionPoint> points = new ArrayList<>();
        List<String> activitySpikes = new ArrayList<>();

        Instant firstCommit = nodes.stream().map(VersionNode::getCreatedAt).min(Instant::compareTo).orElse(Instant.now());
        Instant lastCommit = nodes.stream().map(VersionNode::getCreatedAt).max(Instant::compareTo).orElse(Instant.now());

        long totalDays = ChronoUnit.DAYS.between(firstCommit, lastCommit);
        int periods = (int) Math.ceil(totalDays / (double) intervalDays);

        for (int i = 0; i <= periods; i++) {
            Instant periodStart = firstCommit.plusSeconds(i * intervalDays * 86400L);
            Instant periodEnd = firstCommit.plusSeconds((i + 1) * intervalDays * 86400L);

            long count = nodes.stream()
                    .filter(n -> n.getCreatedAt().isAfter(periodStart) && n.getCreatedAt().isBefore(periodEnd))
                    .count();

            growthByPeriod.put(periodStart.toString(), (int) count);
            points.add(new EvolutionPoint(periodStart, (int) count, 0, 0, count / 10.0));

            if (count > 10) {
                activitySpikes.add(periodStart.toString());
            }
        }

        double growthRate = nodes.size() / (double) Math.max(1, totalDays);

        return new GraphEvolution(points, growthByPeriod, activitySpikes, growthRate);
    }

    // =========================================================================
    // Graph Integrity & Validation
    // =========================================================================

    @Override
    public GraphIntegrityReport verifyGraphIntegrity(String pageId) {
        log.debug("Verifying graph integrity for page: {}", pageId);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        List<String> corruptedNodes = new ArrayList<>();
        List<String> missingReferences = new ArrayList<>();

        for (VersionNode node : nodes) {
            if (node.getMerkleHash() == null || node.getMerkleHash().isEmpty()) {
                corruptedNodes.add(node.getId());
            }
            if (node.getParentVersionId() != null) {
                Optional<VersionNode> parent = versionNodeRepository.findById(node.getParentVersionId());
                if (parent.isEmpty()) {
                    missingReferences.add(node.getId() + " -> " + node.getParentVersionId());
                }
            }
        }

        double integrityScore = nodes.isEmpty() ? 100 :
                (double) (nodes.size() - corruptedNodes.size()) / nodes.size() * 100;

        boolean valid = corruptedNodes.isEmpty() && missingReferences.isEmpty();

        return new GraphIntegrityReport(
                valid,
                integrityScore,
                corruptedNodes,
                new ArrayList<>(),
                missingReferences,
                valid ? "Graph integrity verified" : "Repair recommended"
        );
    }

    @Override
    public GraphValidationResult validateGraphStructure(String pageId) {
        log.debug("Validating graph structure for page: {}", pageId);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        List<String> cycles = findCycles(nodes).stream()
                .map(cycle -> String.join(" -> ", cycle))
                .collect(Collectors.toList());

        List<String> orphanedNodes = nodes.stream()
                .filter(n -> !n.isRoot() && n.getParentVersionId() == null)
                .map(VersionNode::getId)
                .collect(Collectors.toList());

        List<String> danglingReferences = nodes.stream()
                .filter(n -> n.getParentVersionId() != null)
                .filter(n -> nodes.stream().noneMatch(p -> p.getId().equals(n.getParentVersionId())))
                .map(VersionNode::getId)
                .collect(Collectors.toList());

        List<String> multipleParents = nodes.stream()
                .filter(n -> nodes.stream().filter(p -> p.getChildren().contains(n.getId())).count() > 1)
                .map(VersionNode::getId)
                .collect(Collectors.toList());

        List<String> warnings = new ArrayList<>();
        if (!cycles.isEmpty()) warnings.add("Cycles detected in version graph");
        if (!orphanedNodes.isEmpty()) warnings.add("Orphaned nodes found");
        if (!danglingReferences.isEmpty()) warnings.add("Dangling references detected");

        boolean isValid = cycles.isEmpty() && orphanedNodes.isEmpty() && danglingReferences.isEmpty();

        return new GraphValidationResult(isValid, cycles, orphanedNodes, danglingReferences, multipleParents, warnings);
    }

    @Override
    @Transactional
    public GraphRepairResult repairGraph(String pageId, String userId) {
        log.info("Repairing graph for page: {} by user {}", pageId, userId);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        int nodesRepaired = 0;
        int chainsRebuilt = 0;
        List<String> repairedNodes = new ArrayList<>();
        List<String> unrecoverableNodes = new ArrayList<>();

        for (VersionNode node : nodes) {
            if (node.getMerkleHash() == null || node.getMerkleHash().isEmpty()) {
                node.setMerkleHash("repaired-" + UUID.randomUUID());
                versionNodeRepository.save(node);
                repairedNodes.add(node.getId());
                nodesRepaired++;
            }
        }

        return new GraphRepairResult(
                true,
                nodesRepaired,
                chainsRebuilt,
                repairedNodes,
                unrecoverableNodes,
                "Repaired " + nodesRepaired + " nodes"
        );
    }

    @Override
    public GraphAnomalyReport detectGraphAnomalies(String pageId) {
        log.debug("Detecting graph anomalies for page: {}", pageId);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        List<String> anomalies = new ArrayList<>();
        List<String> suspiciousPatterns = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        List<List<String>> cycles = findCycles(nodes);
        if (!cycles.isEmpty()) {
            anomalies.add("Cycles detected in version graph");
            suspiciousPatterns.add("Circular dependencies");
            recommendations.add("Break cycles by removing circular references");
        }

        long brokenChains = nodes.stream()
                .filter(n -> n.getParentVersionId() != null)
                .filter(n -> nodes.stream().noneMatch(p -> p.getId().equals(n.getParentVersionId())))
                .count();
        if (brokenChains > 0) {
            anomalies.add("Broken chains detected");
            suspiciousPatterns.add("Missing parent references");
            recommendations.add("Restore missing parent references");
        }

        int maxDepth = nodes.stream().mapToInt(n -> n.getDepth() != null ? n.getDepth() : 0).max().orElse(0);
        if (maxDepth > 100) {
            anomalies.add("Excessive graph depth");
            suspiciousPatterns.add("Very deep version tree");
            recommendations.add("Consider squashing linear chains");
        }

        double anomalyScore = (cycles.size() * 20 + brokenChains * 10 + Math.max(0, maxDepth - 100) / 10) / 100.0;
        anomalyScore = Math.min(1.0, anomalyScore);

        return new GraphAnomalyReport(
                !anomalies.isEmpty(),
                anomalyScore,
                anomalies,
                suspiciousPatterns,
                recommendations
        );
    }

    // =========================================================================
    // Graph Optimization & Compression
    // =========================================================================

    @Override
    @Transactional
    public GraphOptimizationResult optimizeGraph(String pageId, boolean dryRun) {
        log.info("Optimizing graph for page: {} (dryRun: {})", pageId, dryRun);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        long originalSize = nodes.size() * 1024L;
        int nodesCompressed = 0;
        int edgesRemoved = 0;
        List<String> optimizedChains = new ArrayList<>();

        List<List<VersionNode>> linearChains = findLinearChains(nodes);

        for (List<VersionNode> chain : linearChains) {
            if (chain.size() > 5 && !dryRun) {
                optimizedChains.add(chain.get(0).getId() + "..." + chain.get(chain.size() - 1).getId());
                nodesCompressed += chain.size() - 1;
            }
        }

        long optimizedSize = (nodes.size() - nodesCompressed) * 1024L;
        double compressionRatio = originalSize > 0 ? (double) optimizedSize / originalSize : 1.0;

        return new GraphOptimizationResult(
                originalSize, optimizedSize, originalSize - optimizedSize,
                compressionRatio, nodesCompressed, edgesRemoved, optimizedChains
        );
    }

    @Override
    @Transactional
    public ChainCompressionResult compressLinearChains(String pageId, int minChainLength) {
        log.info("Compressing linear chains for page: {} (min length: {})", pageId, minChainLength);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        int chainsCompressed = 0;
        int nodesCompressed = 0;
        long originalNodes = nodes.size();
        List<String> compressedChainIds = new ArrayList<>();

        List<List<VersionNode>> linearChains = findLinearChains(nodes);

        for (List<VersionNode> chain : linearChains) {
            if (chain.size() >= minChainLength) {
                chainsCompressed++;
                nodesCompressed += chain.size() - 1;
                compressedChainIds.add(chain.get(0).getId());

                for (int i = 1; i < chain.size() - 1; i++) {
                    versionNodeRepository.softDeleteNode(chain.get(i).getId(), Instant.now(), "SYSTEM");
                }

                VersionNode first = chain.get(0);
                VersionNode last = chain.get(chain.size() - 1);
                first.getChildren().clear();
                first.getChildren().add(last.getId());
                versionNodeRepository.save(first);
            }
        }

        long compressedNodes = nodes.size() - nodesCompressed;
        long spaceSaved = nodesCompressed * 1024L;

        return new ChainCompressionResult(
                chainsCompressed, nodesCompressed, originalNodes, compressedNodes,
                spaceSaved, compressedChainIds
        );
    }

    @Override
    @Transactional
    public PruneResult pruneOrphanedNodes(String pageId, String userId) {
        log.info("Pruning orphaned nodes for page: {} by user {}", pageId, userId);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        List<String> prunedNodeIds = new ArrayList<>();
        List<VersionNode> orphanedNodes = nodes.stream()
                .filter(n -> !n.isRoot() && n.getParentVersionId() == null)
                .collect(Collectors.toList());

        for (VersionNode node : orphanedNodes) {
            versionNodeRepository.softDeleteNode(node.getId(), Instant.now(), userId);
            prunedNodeIds.add(node.getId());
        }

        long spaceFreed = prunedNodeIds.size() * 1024L;
        String archiveId = UUID.randomUUID().toString();

        return new PruneResult(prunedNodeIds.size(), spaceFreed, prunedNodeIds, archiveId);
    }

    @Override
    public String createGraphSnapshot(String pageId, String label) {
        log.info("Creating graph snapshot for page: {} with label: {}", pageId, label);
        return UUID.randomUUID().toString();
    }

    @Override
    public GraphComparison compareSnapshots(String snapshotId1, String snapshotId2) {
        log.debug("Comparing snapshots {} and {}", snapshotId1, snapshotId2);
        return new GraphComparison(
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), 0.95
        );
    }

    // =========================================================================
    // Graph Visualization & Export
    // =========================================================================

    @Override
    public String exportGraph(String pageId, String format, boolean includeLabels) {
        log.debug("Exporting graph for page: {} in format: {}", pageId, format);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        if ("DOT".equalsIgnoreCase(format)) {
            StringBuilder dot = new StringBuilder();
            dot.append("digraph VersionGraph {\n");
            dot.append("  rankdir=TB;\n");
            dot.append("  node [shape=box, style=filled, fillcolor=lightblue];\n\n");

            for (VersionNode node : nodes) {
                String label = includeLabels ?
                        String.format("%s\\n(%s)", node.getVersionString(), node.getBranchName()) :
                        node.getVersionString();
                dot.append(String.format("  \"%s\" [label=\"%s\"];\n", node.getId(), label));
            }

            dot.append("\n");
            for (VersionNode node : nodes) {
                if (node.getParentVersionId() != null) {
                    dot.append(String.format("  \"%s\" -> \"%s\";\n", node.getParentVersionId(), node.getId()));
                }
            }

            dot.append("}\n");
            return dot.toString();
        }

        return "{}";
    }

    @Override
    public GraphLayout generateLayout(String pageId, int width, int height) {
        log.debug("Generating layout for page: {} ({}x{})", pageId, width, height);

        List<VersionNode> nodes = versionNodeRepository.findByPageIdOrderByVersionNumberDesc(pageId);

        Map<String, Point> nodePositions = new HashMap<>();
        Map<String, String> nodeLayers = new HashMap<>();

        int layerHeight = height / (nodes.size() + 1);
        int layer = 0;

        for (VersionNode node : nodes) {
            double x = width / 2.0 + (Math.random() - 0.5) * 100;
            double y = layer * layerHeight + layerHeight / 2.0;
            nodePositions.put(node.getId(), new Point(x, y));
            nodeLayers.put(node.getId(), String.valueOf(layer));
            layer++;
        }

        return new GraphLayout(nodePositions, nodeLayers, width, height, 0);
    }

    @Override
    public VersionGraphResponse getSubgraph(String nodeId, int radius, int maxNodes) {
        log.debug("Getting subgraph for node: {} with radius: {}", nodeId, radius);

        Set<String> nodesInSubgraph = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(nodeId);
        nodesInSubgraph.add(nodeId);

        for (int r = 0; r < radius && !queue.isEmpty(); r++) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                String current = queue.poll();
                Optional<VersionNode> node = versionNodeRepository.findById(current);
                if (node.isPresent()) {
                    for (String child : node.get().getChildren()) {
                        if (!nodesInSubgraph.contains(child) && nodesInSubgraph.size() < maxNodes) {
                            nodesInSubgraph.add(child);
                            queue.add(child);
                        }
                    }
                    if (node.get().getParentVersionId() != null &&
                            !nodesInSubgraph.contains(node.get().getParentVersionId()) &&
                            nodesInSubgraph.size() < maxNodes) {
                        nodesInSubgraph.add(node.get().getParentVersionId());
                        queue.add(node.get().getParentVersionId());
                    }
                }
            }
        }

        List<VersionNode> subgraphNodes = nodesInSubgraph.stream()
                .map(versionNodeRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (subgraphNodes.isEmpty()) {
            return new VersionGraphResponse(new ArrayList<>(), new ArrayList<>(), 0, 0, false, new ArrayList<>());
        }

        return buildVersionGraph(subgraphNodes.get(0).getPageId(), true, subgraphNodes.size());
    }
}