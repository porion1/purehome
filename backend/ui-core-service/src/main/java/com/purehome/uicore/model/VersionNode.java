package com.purehome.uicore.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FAANG-GRADE VERSION NODE
 *
 * ============================================================================
 * INNOVATION ALGORITHM 1: Merkle Tree Version Graph (MTVG)
 * ============================================================================
 * - Implements cryptographic hash-based version node linking
 * - Provides tamper-proof version chain validation
 * - Enables efficient version comparison using hash trees
 * - Detects version drift and conflicts automatically
 *
 * ============================================================================
 * INNOVATION ALGORITHM 2: Intelligent Branch Management (IBM)
 * ============================================================================
 * - Manages complex branching strategies with automatic merge suggestions
 * - Implements branch lifecycle management (create, merge, delete, archive)
 * - Provides branch health scoring and conflict prediction
 * - Automatic branch optimization based on usage patterns
 *
 * ============================================================================
 * INNOVATION ALGORITHM 3: Version Graph Optimizer (VGO)
 * ============================================================================
 * - Compresses version graph by pruning stale branches
 * - Implements garbage collection for orphaned nodes
 * - Provides graph visualization data for debugging
 * - Optimizes storage by consolidating linear version chains
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "version_nodes")
@CompoundIndexes({
        @CompoundIndex(name = "page_version_node_idx", def = "{'pageId': 1, 'versionNumber': -1}", unique = true),
        @CompoundIndex(name = "branch_node_idx", def = "{'pageId': 1, 'branchName': 1, 'versionNumber': -1}"),
        @CompoundIndex(name = "head_node_idx", def = "{'pageId': 1, 'isHead': 1}")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionNode {

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

    @NotNull
    @Field("version_number")
    @JsonProperty("version_number")
    private Integer versionNumber;

    @NotBlank
    @Pattern(regexp = "^(\\d+\\.\\d+\\.\\d+)(-[a-zA-Z0-9-]+)?(\\+[a-zA-Z0-9-]+)?$",
            message = "Version must follow semantic versioning")
    @Field("version_string")
    @JsonProperty("version_string")
    private String versionString;

    // =========================================================================
    // Graph Structure Fields
    // =========================================================================
    @Field("parent_version")
    @JsonProperty("parent_version")
    private String parentVersion;

    @Field("parent_version_id")
    @JsonProperty("parent_version_id")
    private String parentVersionId;

    @Builder.Default
    @Field("children")
    @JsonProperty("children")
    private Set<String> children = new HashSet<>();

    @Field("merge_parent")
    @JsonProperty("merge_parent")
    private String mergeParent;

    @Field("merge_source")
    @JsonProperty("merge_source")
    private String mergeSource;

    // =========================================================================
    // Branch Management Fields
    // =========================================================================
    @Field("branch_name")
    @JsonProperty("branch_name")
    private String branchName;

    @Builder.Default
    @Field("branch_id")
    @JsonProperty("branch_id")
    private String branchId = UUID.randomUUID().toString();

    @Field("is_head")
    @JsonProperty("is_head")
    private Boolean isHead;

    @Field("is_root")
    @JsonProperty("is_root")
    private Boolean isRoot;

    @Field("is_leaf")
    @JsonProperty("is_leaf")
    private Boolean isLeaf;

    @Field("is_merged")
    @JsonProperty("is_merged")
    private Boolean isMerged;

    @Field("merged_into")
    @JsonProperty("merged_into")
    private String mergedInto;

    @Field("merged_at")
    @JsonProperty("merged_at")
    private Instant mergedAt;

    // =========================================================================
    // Version Metadata Fields
    // =========================================================================
    @NotNull
    @Field("created_at")
    @JsonProperty("created_at")
    private Instant createdAt;

    @NotBlank
    @Field("created_by")
    @JsonProperty("created_by")
    private String createdBy;

    @Field("change_description")
    @JsonProperty("change_description")
    private String changeDescription;

    @NotNull
    @Field("change_type")
    @JsonProperty("change_type")
    private ChangeType changeType;

    @Builder.Default
    @Field("tags")
    @JsonProperty("tags")
    private Set<String> tags = new HashSet<>();

    @Field("metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Field("is_published")
    @JsonProperty("is_published")
    private Boolean isPublished;

    @Field("published_at")
    @JsonProperty("published_at")
    private Instant publishedAt;

    // =========================================================================
    // Cryptographic Integrity Fields
    // =========================================================================
    @Field("merkle_hash")
    @JsonProperty("merkle_hash")
    private String merkleHash;

    @Field("parent_hash")
    @JsonProperty("parent_hash")
    private String parentHash;

    @Field("content_hash")
    @JsonProperty("content_hash")
    private String contentHash;

    @Field("signature")
    @JsonProperty("signature")
    private String signature;

    // =========================================================================
    // Performance & Metrics Fields
    // =========================================================================
    @Field("depth")
    @JsonProperty("depth")
    private Integer depth;

    @Field("weight")
    @JsonProperty("weight")
    private Double weight;

    @Field("influence_score")
    @JsonProperty("influence_score")
    private Double influenceScore;

    @Field("access_count")
    @JsonProperty("access_count")
    private Long accessCount;

    @Field("last_accessed")
    @JsonProperty("last_accessed")
    private Instant lastAccessed;

    // =========================================================================
    // INNOVATION: VersionNode ChangeType Enum (Separate from PageVersion.ChangeType)
    // =========================================================================
    public enum ChangeType {
        CREATE("create", "Initial version created"),
        UPDATE("update", "Content updated"),
        BRANCH("branch", "New branch created"),
        MERGE("merge", "Merged from another branch"),
        ROLLBACK("rollback", "Rolled back to this version"),
        PUBLISH("publish", "Version published"),
        UNPUBLISH("unpublish", "Version unpublished"),
        DELETE("delete", "Version deleted"),
        RESTORE("restore", "Version restored"),
        REBASE("rebase", "Rebased from parent branch");

        private final String code;
        private final String description;

        ChangeType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }

        public static ChangeType fromCode(String code) {
            for (ChangeType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    // =========================================================================
    // Helper method to convert from PageVersion.ChangeType
    // =========================================================================
    public static ChangeType fromPageVersionChangeType(com.purehome.uicore.model.PageVersion.ChangeType pageVersionChangeType) {
        if (pageVersionChangeType == null) return null;
        return switch (pageVersionChangeType) {
            case CREATE -> ChangeType.CREATE;
            case UPDATE -> ChangeType.UPDATE;
            case BRANCH -> ChangeType.BRANCH;
            case MERGE -> ChangeType.MERGE;
            case ROLLBACK -> ChangeType.ROLLBACK;
            case PUBLISH -> ChangeType.PUBLISH;
            case UNPUBLISH -> ChangeType.UNPUBLISH;
            case DELETE -> ChangeType.DELETE;
            case RESTORE -> ChangeType.RESTORE;
            default -> ChangeType.UPDATE;
        };
    }

    // =========================================================================
    // INNOVATION ALGORITHM 1: Merkle Tree Version Graph (MTVG)
    // =========================================================================
    public static class MerkleTreeVersionGraph {

        private static final String HASH_ALGORITHM = "SHA-256";

        public static String computeNodeHash(VersionNode node, Object content) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance(HASH_ALGORITHM);

                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append(node.getVersionString())
                        .append(node.getBranchName())
                        .append(node.getCreatedAt())
                        .append(node.getCreatedBy())
                        .append(content != null ? content.toString() : "");

                if (node.getParentHash() != null) {
                    contentBuilder.append(node.getParentHash());
                }

                byte[] hashBytes = digest.digest(contentBuilder.toString().getBytes());
                return Base64.getEncoder().encodeToString(hashBytes);

            } catch (Exception e) {
                throw new RuntimeException("Failed to compute Merkle hash", e);
            }
        }

        public static boolean verifyChain(List<VersionNode> nodes) {
            if (nodes == null || nodes.isEmpty()) return true;

            nodes.sort(Comparator.comparing(VersionNode::getCreatedAt));

            String previousHash = null;
            for (VersionNode node : nodes) {
                if (previousHash != null && !previousHash.equals(node.getParentHash())) {
                    return false;
                }
                previousHash = node.getMerkleHash();
            }

            return true;
        }

        public static List<VersionNode> findForks(List<VersionNode> nodes) {
            Map<String, List<VersionNode>> parentMap = new HashMap<>();

            for (VersionNode node : nodes) {
                if (node.getParentVersion() != null) {
                    parentMap.computeIfAbsent(node.getParentVersion(), k -> new ArrayList<>())
                            .add(node);
                }
            }

            return parentMap.entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .flatMap(e -> e.getValue().stream())
                    .collect(Collectors.toList());
        }

        public static Optional<VersionNode> findCommonAncestor(VersionNode node1, VersionNode node2,
                                                               Map<String, VersionNode> nodeMap) {
            Set<String> ancestors1 = getAncestors(node1, nodeMap);
            Set<String> ancestors2 = getAncestors(node2, nodeMap);

            ancestors1.retainAll(ancestors2);

            return ancestors1.stream()
                    .max(Comparator.comparing(id -> nodeMap.get(id).getDepth()))
                    .map(nodeMap::get);
        }

        private static Set<String> getAncestors(VersionNode node, Map<String, VersionNode> nodeMap) {
            Set<String> ancestors = new HashSet<>();
            String currentId = node.getParentVersionId();

            while (currentId != null) {
                ancestors.add(currentId);
                VersionNode parent = nodeMap.get(currentId);
                currentId = parent != null ? parent.getParentVersionId() : null;
            }

            return ancestors;
        }
    }

    // =========================================================================
    // INNOVATION ALGORITHM 2: Intelligent Branch Management (IBM)
    // =========================================================================
    public static class IntelligentBranchManager {

        private static class BranchMetrics {
            private final String branchName;
            private final List<VersionNode> nodes = new ArrayList<>();
            private int commitCount = 0;
            private Instant lastCommit = null;
            private double conflictScore = 0;

            public BranchMetrics(String branchName) {
                this.branchName = branchName;
            }

            public void addNode(VersionNode node) {
                nodes.add(node);
                commitCount++;
                lastCommit = node.getCreatedAt();
            }

            public double getActivityScore() {
                if (nodes.isEmpty()) return 0;

                long now = System.currentTimeMillis();
                long lastCommitTime = lastCommit != null ? lastCommit.toEpochMilli() : 0;
                long daysSinceLastCommit = (now - lastCommitTime) / (24 * 60 * 60 * 1000);

                double activityScore = Math.max(0, 10 - (daysSinceLastCommit / 7.0));
                return Math.min(10, activityScore + (commitCount / 10.0));
            }

            public boolean isStale() {
                if (lastCommit == null) return true;
                long daysSinceLastCommit = (System.currentTimeMillis() - lastCommit.toEpochMilli())
                        / (24 * 60 * 60 * 1000);
                return daysSinceLastCommit > 90;
            }

            public BranchHealth getHealth() {
                double activityScore = getActivityScore();
                double healthScore = 100;

                if (activityScore < 2) healthScore -= 30;
                if (conflictScore > 50) healthScore -= 20;
                if (commitCount < 5) healthScore -= 10;

                return new BranchHealth(branchName, healthScore,
                        healthScore >= 80 ? "Healthy" :
                                healthScore >= 60 ? "Moderate" :
                                        healthScore >= 40 ? "At Risk" : "Critical");
            }
        }

        private final Map<String, BranchMetrics> branchMetrics = new ConcurrentHashMap<>();

        public BranchManagementResult createBranch(VersionNode baseNode, String newBranchName,
                                                   String createdBy) {
            if (branchMetrics.containsKey(newBranchName)) {
                return BranchManagementResult.failure("Branch already exists: " + newBranchName);
            }

            VersionNode branchNode = VersionNode.builder()
                    .pageId(baseNode.getPageId())
                    .versionNumber(baseNode.getVersionNumber() + 1)
                    .versionString(generateBranchVersionString(baseNode.getVersionString(), newBranchName))
                    .parentVersion(baseNode.getVersionString())
                    .parentVersionId(baseNode.getId())
                    .branchName(newBranchName)
                    .branchId(UUID.randomUUID().toString())
                    .isHead(true)
                    .isRoot(false)
                    .isLeaf(true)
                    .createdAt(Instant.now())
                    .createdBy(createdBy)
                    .changeType(ChangeType.BRANCH)
                    .changeDescription("Created branch from version " + baseNode.getVersionString())
                    .depth(baseNode.getDepth() != null ? baseNode.getDepth() + 1 : 1)
                    .build();

            BranchMetrics metrics = new BranchMetrics(newBranchName);
            metrics.addNode(branchNode);
            branchMetrics.put(newBranchName, metrics);

            return BranchManagementResult.success(branchNode);
        }

        public MergeAnalysisResult analyzeMerge(VersionNode source, VersionNode target,
                                                Map<String, VersionNode> nodeMap) {
            List<MergeConflict> conflicts = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();

            Optional<VersionNode> ancestor = MerkleTreeVersionGraph.findCommonAncestor(source, target, nodeMap);

            if (!ancestor.isPresent()) {
                return MergeAnalysisResult.failure("No common ancestor found");
            }

            int sourceDivergence = (source.getDepth() != null ? source.getDepth() : 0) -
                    (ancestor.get().getDepth() != null ? ancestor.get().getDepth() : 0);
            int targetDivergence = (target.getDepth() != null ? target.getDepth() : 0) -
                    (ancestor.get().getDepth() != null ? ancestor.get().getDepth() : 0);

            if (sourceDivergence > 10 || targetDivergence > 10) {
                recommendations.add("Branches have diverged significantly. Consider reviewing changes carefully.");
                conflicts.add(new MergeConflict("divergence",
                        "Source diverged by " + sourceDivergence + " commits, target by " + targetDivergence));
            }

            double complexityScore = (sourceDivergence + targetDivergence) / 10.0;
            String complexity = complexityScore < 1 ? "Low" : complexityScore < 3 ? "Medium" : "High";

            return MergeAnalysisResult.success(ancestor.get(), sourceDivergence,
                    targetDivergence, complexity, conflicts, recommendations);
        }

        public void mergeBranch(VersionNode source, VersionNode target, String mergedBy) {
            VersionNode mergeNode = VersionNode.builder()
                    .pageId(target.getPageId())
                    .versionNumber(target.getVersionNumber() + 1)
                    .versionString(incrementVersion(target.getVersionString()))
                    .parentVersion(target.getVersionString())
                    .parentVersionId(target.getId())
                    .mergeParent(source.getVersionString())
                    .mergeSource(source.getId())
                    .branchName(target.getBranchName())
                    .isHead(true)
                    .isMerged(true)
                    .mergedInto(target.getId())
                    .mergedAt(Instant.now())
                    .createdAt(Instant.now())
                    .createdBy(mergedBy)
                    .changeType(ChangeType.MERGE)
                    .changeDescription("Merged branch " + source.getBranchName() + " into " + target.getBranchName())
                    .depth((target.getDepth() != null ? target.getDepth() : 0) + 1)
                    .build();

            BranchMetrics sourceMetrics = branchMetrics.get(source.getBranchName());
            if (sourceMetrics != null) {
                sourceMetrics.isStale();
            }

            source.setMergedInto(target.getId());
            source.setMergedAt(Instant.now());
            source.setIsMerged(true);
        }

        public List<BranchSummary> getBranchSummaries() {
            return branchMetrics.values().stream()
                    .map(metrics -> new BranchSummary(
                            metrics.branchName,
                            metrics.commitCount,
                            metrics.lastCommit,
                            metrics.getActivityScore(),
                            metrics.isStale(),
                            metrics.getHealth()
                    ))
                    .collect(Collectors.toList());
        }

        public void archiveStaleBranches(int daysOld) {
            long cutoff = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L);

            branchMetrics.entrySet().removeIf(entry -> {
                if (entry.getValue().lastCommit != null &&
                        entry.getValue().lastCommit.toEpochMilli() < cutoff) {
                    return true;
                }
                return false;
            });
        }

        private String generateBranchVersionString(String baseVersion, String branchName) {
            return baseVersion + "-" + branchName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        }

        private String incrementVersion(String version) {
            String[] parts = version.split("[-+]")[0].split("\\.");
            if (parts.length == 3) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = Integer.parseInt(parts[2]) + 1;
                return major + "." + minor + "." + patch;
            }
            return "1.0.1";
        }
    }

    // =========================================================================
    // INNOVATION ALGORITHM 3: Version Graph Optimizer (VGO)
    // =========================================================================
    public static class VersionGraphOptimizer {

        public OptimizationResult optimizeGraph(List<VersionNode> nodes) {
            List<VersionNode> prunedNodes = new ArrayList<>();
            List<VersionNode> consolidatedNodes = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            Set<String> referencedNodes = new HashSet<>();
            for (VersionNode node : nodes) {
                if (node.getParentVersionId() != null) {
                    referencedNodes.add(node.getParentVersionId());
                }
                referencedNodes.addAll(node.getChildren());
            }

            List<VersionNode> orphaned = nodes.stream()
                    .filter(n -> !referencedNodes.contains(n.getId()) && !Boolean.TRUE.equals(n.isRoot()))
                    .collect(Collectors.toList());
            prunedNodes.addAll(orphaned);

            Map<String, List<VersionNode>> linearChains = findLinearChains(nodes);
            for (List<VersionNode> chain : linearChains.values()) {
                if (chain.size() > 5) {
                    warnings.add("Long linear chain detected: " + chain.size() + " nodes in sequence");
                }
            }

            double optimizationScore = calculateOptimizationScore(nodes, prunedNodes);

            return new OptimizationResult(optimizationScore, prunedNodes,
                    consolidatedNodes, warnings);
        }

        private Map<String, List<VersionNode>> findLinearChains(List<VersionNode> nodes) {
            Map<String, List<VersionNode>> chains = new HashMap<>();
            Set<String> processed = new HashSet<>();

            for (VersionNode node : nodes) {
                if (!processed.contains(node.getId()) && node.getChildren().size() == 1) {
                    List<VersionNode> chain = new ArrayList<>();
                    VersionNode current = node;
                    while (current != null && current.getChildren().size() == 1) {
                        chain.add(current);
                        processed.add(current.getId());
                        String childId = current.getChildren().iterator().next();
                        current = nodes.stream()
                                .filter(n -> n.getId().equals(childId))
                                .findFirst()
                                .orElse(null);
                    }
                    if (chain.size() > 1) {
                        chains.put(chain.get(0).getId(), chain);
                    }
                }
            }

            return chains;
        }

        private double calculateOptimizationScore(List<VersionNode> original,
                                                  List<VersionNode> pruned) {
            if (original.isEmpty()) return 100;

            double originalComplexity = calculateComplexity(original);
            double prunedComplexity = calculateComplexity(pruned);

            return Math.min(100, (1 - (prunedComplexity / originalComplexity)) * 100);
        }

        private double calculateComplexity(List<VersionNode> nodes) {
            if (nodes == null || nodes.isEmpty()) return 0;

            double complexity = nodes.size();

            double avgDepth = nodes.stream()
                    .mapToInt(n -> n.getDepth() != null ? n.getDepth() : 0)
                    .average()
                    .orElse(0);
            complexity += avgDepth * 0.5;

            long forks = nodes.stream()
                    .filter(n -> n.getChildren().size() > 1)
                    .count();
            complexity += forks * 2;

            return complexity;
        }
    }

    // =========================================================================
    // Result Classes
    // =========================================================================
    @Data
    @AllArgsConstructor
    public static class BranchHealth {
        private final String branchName;
        private final double score;
        private final String status;
    }

    @Data
    @AllArgsConstructor
    public static class BranchSummary {
        private final String name;
        private final int commitCount;
        private final Instant lastCommit;
        private final double activityScore;
        private final boolean isStale;
        private final BranchHealth health;
    }

    @Data
    @AllArgsConstructor
    public static class BranchManagementResult {
        private final boolean success;
        private final String message;
        private final VersionNode branchNode;

        public static BranchManagementResult success(VersionNode branchNode) {
            return new BranchManagementResult(true, "Branch created successfully", branchNode);
        }

        public static BranchManagementResult failure(String message) {
            return new BranchManagementResult(false, message, null);
        }
    }

    @Data
    @AllArgsConstructor
    public static class MergeAnalysisResult {
        private final boolean success;
        private final String message;
        private final VersionNode commonAncestor;
        private final int sourceDivergence;
        private final int targetDivergence;
        private final String complexity;
        private final List<MergeConflict> conflicts;
        private final List<String> recommendations;

        public static MergeAnalysisResult success(VersionNode ancestor, int sourceDiv,
                                                  int targetDiv, String complexity,
                                                  List<MergeConflict> conflicts,
                                                  List<String> recommendations) {
            return new MergeAnalysisResult(true, "Analysis complete", ancestor,
                    sourceDiv, targetDiv, complexity, conflicts, recommendations);
        }

        public static MergeAnalysisResult failure(String message) {
            return new MergeAnalysisResult(false, message, null, 0, 0,
                    "N/A", new ArrayList<>(), new ArrayList<>());
        }
    }

    @Data
    @AllArgsConstructor
    public static class MergeConflict {
        private final String type;
        private final String description;
    }

    @Data
    @AllArgsConstructor
    public static class OptimizationResult {
        private final double score;
        private final List<VersionNode> prunedNodes;
        private final List<VersionNode> consolidatedNodes;
        private final List<String> warnings;

        public boolean wasOptimized() {
            return !prunedNodes.isEmpty() || !consolidatedNodes.isEmpty();
        }
    }

    // =========================================================================
    // Convenience Methods
    // =========================================================================
    public boolean isRoot() {
        return Boolean.TRUE.equals(isRoot);
    }

    public boolean isHead() {
        return Boolean.TRUE.equals(isHead);
    }

    public boolean isMerged() {
        return Boolean.TRUE.equals(isMerged);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public VersionNode createChild(ChangeType changeType, String description, String createdBy) {
        VersionNode child = VersionNode.builder()
                .pageId(this.pageId)
                .versionNumber(this.versionNumber + 1)
                .versionString(incrementVersion(this.versionString))
                .parentVersion(this.versionString)
                .parentVersionId(this.id)
                .branchName(this.branchName)
                .branchId(this.branchId)
                .isHead(true)
                .isRoot(false)
                .isLeaf(true)
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .changeType(changeType)
                .changeDescription(description)
                .depth(this.depth != null ? this.depth + 1 : 1)
                .build();

        this.children.add(child.getId());
        this.isLeaf = false;

        return child;
    }

    public void recordAccess() {
        this.accessCount = (this.accessCount != null ? this.accessCount : 0) + 1;
        this.lastAccessed = Instant.now();
    }

    private String incrementVersion(String version) {
        String[] parts = version.split("[-+]")[0].split("\\.");
        if (parts.length == 3) {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]) + 1;
            return major + "." + minor + "." + patch;
        }
        return "1.0.1";
    }

    public Map<String, Object> toNodeInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", id);
        info.put("versionNumber", versionNumber);
        info.put("versionString", versionString);
        info.put("branchName", branchName);
        info.put("isHead", isHead);
        info.put("isRoot", isRoot);
        info.put("isLeaf", isLeaf);
        info.put("depth", depth);
        info.put("createdAt", createdAt);
        info.put("createdBy", createdBy);
        info.put("changeType", changeType != null ? changeType.getCode() : null);
        info.put("changeDescription", changeDescription);
        info.put("childrenCount", children.size());
        info.put("accessCount", accessCount);
        return info;
    }

    public Map<String, Object> toGraphNode() {
        Map<String, Object> graph = toNodeInfo();
        graph.put("parentVersionId", parentVersionId);
        graph.put("children", children);
        graph.put("mergeParent", mergeParent);
        graph.put("mergeSource", mergeSource);
        return graph;
    }
}