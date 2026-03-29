package com.purehome.uicore.repository;

import com.purehome.uicore.model.VersionNode;
import com.purehome.uicore.model.VersionNode.ChangeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * FAANG-GRADE VERSION NODE REPOSITORY
 *
 * Manages version graph with Merkle tree integrity verification
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Repository
public interface VersionNodeRepository extends MongoRepository<VersionNode, String> {

    // =========================================================================
    // Core Version Node Operations
    // =========================================================================

    /**
     * Find all version nodes for a page
     */
    @Query("{ 'pageId': ?0, 'deleted': { $ne: true } }")
    List<VersionNode> findByPageIdOrderByVersionNumberDesc(String pageId);

    /**
     * Find version node by page and version number
     */
    @Query("{ 'pageId': ?0, 'versionNumber': ?1, 'deleted': { $ne: true } }")
    Optional<VersionNode> findByPageIdAndVersionNumber(String pageId, Integer versionNumber);

    /**
     * Find head version of a branch
     */
    @Query("{ 'pageId': ?0, 'branchName': ?1, 'isHead': true, 'deleted': { $ne: true } }")
    Optional<VersionNode> findBranchHead(String pageId, String branchName);

    /**
     * Find root version
     */
    @Query("{ 'pageId': ?0, 'isRoot': true, 'deleted': { $ne: true } }")
    Optional<VersionNode> findRootVersion(String pageId);

    /**
     * Find all branch heads
     */
    @Query("{ 'pageId': ?0, 'isHead': true, 'deleted': { $ne: true } }")
    List<VersionNode> findAllBranchHeads(String pageId);

    // =========================================================================
    // Graph Traversal Operations
    // =========================================================================

    /**
     * Find children of a version node
     */
    @Query("{ 'parentVersionId': ?0, 'deleted': { $ne: true } }")
    List<VersionNode> findChildren(String parentVersionId);

    /**
     * Find versions in a branch
     */
    @Query("{ 'pageId': ?0, 'branchName': ?1, 'deleted': { $ne: true } }")
    List<VersionNode> findByPageIdAndBranchName(String pageId, String branchName);

    /**
     * Find versions in time range
     */
    @Query("{ 'pageId': ?0, 'createdAt': { $gte: ?1, $lte: ?2 }, 'deleted': { $ne: true } }")
    List<VersionNode> findVersionsInTimeRange(String pageId, Instant start, Instant end);

    // =========================================================================
    // Branch Management
    // =========================================================================

    /**
     * Find stale branches for archival
     */
    @Query("{ 'pageId': ?0, 'branchName': { $ne: 'main' }, 'lastCommit': { $lt: ?1 }, 'deleted': { $ne: true } }")
    List<VersionNode> findStaleBranches(String pageId, Instant cutoffDate);

    /**
     * Unset current head
     */
    @Query("{ 'pageId': ?0, 'branchName': ?1, 'isHead': true }")
    @Update("{ '$set': { 'isHead': false } }")
    void unsetCurrentHead(String pageId, String branchName);

    /**
     * Set as branch head
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'isHead': true, 'lastModifiedDate': ?1 } }")
    void setAsBranchHead(String versionId, Instant modifiedDate);

    // =========================================================================
    // Version Cleanup
    // =========================================================================

    /**
     * Find orphaned nodes
     */
    @Query("{ 'pageId': ?0, 'parentVersionId': { $exists: false }, 'isRoot': false, 'deleted': { $ne: true } }")
    List<VersionNode> findOrphanedNodes(String pageId);

    /**
     * Soft delete node
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'deleted': true, 'deletedAt': ?1, 'deletedBy': ?2 } }")
    void softDeleteNode(String nodeId, Instant deletedAt, String deletedBy);

    /**
     * Hard delete old nodes
     */
    @Query(value = "{ 'deleted': true, 'deletedAt': { $lt: ?0 } }", delete = true)
    void hardDeleteOldNodes(Instant cutoffDate);

    // =========================================================================
    // Version Analytics
    // =========================================================================

    /**
     * Get version graph metrics
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'pageId': ?0, 'deleted': { $ne: true } } }",
            "{ '$group': { " +
                    "  '_id': null, " +
                    "  'totalNodes': { '$sum': 1 }, " +
                    "  'totalBranches': { '$addToSet': '$branchName' }, " +
                    "  'maxDepth': { '$max': '$depth' }, " +
                    "  'avgDepth': { '$avg': '$depth' } " +
                    "} }",
            "{ '$project': { " +
                    "  'totalNodes': 1, " +
                    "  'totalBranches': { '$size': '$totalBranches' }, " +
                    "  'maxDepth': 1, " +
                    "  'avgDepth': 1 " +
                    "} }"
    })
    Optional<GraphMetrics> getGraphMetrics(String pageId);

    /**
     * Get branch health scores
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'pageId': ?0, 'deleted': { $ne: true } } }",
            "{ '$group': { " +
                    "  '_id': '$branchName', " +
                    "  'commitCount': { '$sum': 1 }, " +
                    "  'avgDepth': { '$avg': '$depth' }, " +
                    "  'lastCommit': { '$max': '$createdAt' }, " +
                    "  'firstCommit': { '$min': '$createdAt' } " +
                    "} }",
            "{ '$addFields': { " +
                    "  'activityScore': { '$divide': [ '$commitCount', " +
                    "    { '$max': [1, { '$divide': [ { '$subtract': ['$lastCommit', '$firstCommit'] }, 86400000 ] } ] } ] } " +
                    "} }",
            "{ '$addFields': { " +
                    "  'healthScore': { '$multiply': ['$activityScore', 50] } " +
                    "} }"
    })
    List<BranchHealth> getBranchHealthScores(String pageId);

    // =========================================================================
    // Integrity Verification
    // =========================================================================

    /**
     * Find corrupted nodes
     */
    @Query("{ 'pageId': ?0, 'deleted': { $ne: true }, 'merkleHash': { $exists: true } }")
    List<VersionNode> findCorruptedNodes(String pageId);
}

// =========================================================================
// DTO Classes
// =========================================================================

/**
 * Graph metrics result
 */
class GraphMetrics {
    private int totalNodes;
    private int totalBranches;
    private int maxDepth;
    private double avgDepth;

    public int getTotalNodes() { return totalNodes; }
    public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }
    public int getTotalBranches() { return totalBranches; }
    public void setTotalBranches(int totalBranches) { this.totalBranches = totalBranches; }
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
    public double getAvgDepth() { return avgDepth; }
    public void setAvgDepth(double avgDepth) { this.avgDepth = avgDepth; }
}

/**
 * Branch health result
 */
class BranchHealth {
    private String branchName;
    private int commitCount;
    private double avgDepth;
    private Instant lastCommit;
    private double activityScore;
    private double healthScore;

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public int getCommitCount() { return commitCount; }
    public void setCommitCount(int commitCount) { this.commitCount = commitCount; }
    public double getAvgDepth() { return avgDepth; }
    public void setAvgDepth(double avgDepth) { this.avgDepth = avgDepth; }
    public Instant getLastCommit() { return lastCommit; }
    public void setLastCommit(Instant lastCommit) { this.lastCommit = lastCommit; }
    public double getActivityScore() { return activityScore; }
    public void setActivityScore(double activityScore) { this.activityScore = activityScore; }
    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }

    public String getHealthStatus() {
        if (healthScore >= 80) return "HEALTHY";
        if (healthScore >= 60) return "MODERATE";
        if (healthScore >= 40) return "AT_RISK";
        return "CRITICAL";
    }
}