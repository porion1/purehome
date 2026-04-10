package com.purehome.uicore.repository;

import com.purehome.uicore.model.PageVersion;
import com.purehome.uicore.model.PageVersion.ChangeType;
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
 * FAANG-GRADE PAGE VERSION REPOSITORY
 *
 * Manages version history, branches, and version comparisons
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Repository
public interface PageVersionRepository extends MongoRepository<PageVersion, String> {

    // =========================================================================
    // Core Version Operations
    // =========================================================================

    /**
     * Find all versions for a page, ordered by version number descending
     */
    @Query("{ 'pageId': ?0, 'deleted': { $ne: true } }")
    List<PageVersion> findByPageIdOrderByVersionNumberDesc(String pageId);

    /**
     * Find versions with pagination
     */
    @Query("{ 'pageId': ?0, 'deleted': { $ne: true } }")
    Slice<PageVersion> findByPageId(String pageId, Pageable pageable);

    /**
     * Find specific version by page and version number
     */
    @Query("{ 'pageId': ?0, 'versionNumber': ?1, 'deleted': { $ne: true } }")
    Optional<PageVersion> findByPageIdAndVersionNumber(String pageId, Integer versionNumber);

    /**
     * Find current version (head) of a page
     */
    @Query("{ 'pageId': ?0, 'isCurrent': true, 'deleted': { $ne: true } }")
    Optional<PageVersion> findCurrentVersion(String pageId);

    /**
     * Find published version of a page
     */
    @Query("{ 'pageId': ?0, 'isPublished': true, 'deleted': { $ne: true } }")
    Optional<PageVersion> findPublishedVersion(String pageId);

    // =========================================================================
    // Branch Management Operations
    // =========================================================================

    /**
     * Find all branches for a page
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'pageId': ?0, 'deleted': { $ne: true } } }",
            "{ '$group': { " +
                    "  '_id': '$branchName', " +
                    "  'latestVersion': { '$max': '$versionNumber' }, " +
                    "  'latestVersionId': { '$last': '$_id' }, " +
                    "  'lastModified': { '$max': '$createdAt' } " +
                    "} }",
            "{ '$sort': { 'lastModified': -1 } }"
    })
    List<BranchSummary> findBranchesByPageId(String pageId);

    /**
     * Find versions in a specific branch
     */
    @Query("{ 'pageId': ?0, 'branchName': ?1, 'deleted': { $ne: true } }")
    List<PageVersion> findByPageIdAndBranchName(String pageId, String branchName);

    /**
     * Find head version of a specific branch
     */
    @Query("{ 'pageId': ?0, 'branchName': ?1, 'isHead': true, 'deleted': { $ne: true } }")
    Optional<PageVersion> findBranchHead(String pageId, String branchName);

    // =========================================================================
    // Version Graph Operations
    // =========================================================================

    /**
     * Find all children of a version
     */
    @Query("{ 'parentVersionId': ?0, 'deleted': { $ne: true } }")
    List<PageVersion> findChildrenByParentVersionId(String parentVersionId);

    /**
     * Find versions in date range
     */
    @Query("{ 'pageId': ?0, 'createdAt': { $gte: ?1, $lte: ?2 }, 'deleted': { $ne: true } }")
    List<PageVersion> findVersionsInDateRange(String pageId, Instant start, Instant end);

    // =========================================================================
    // Change Type Operations
    // =========================================================================

    /**
     * Find versions by change type
     */
    @Query("{ 'pageId': ?0, 'changeType': ?1, 'deleted': { $ne: true } }")
    List<PageVersion> findByPageIdAndChangeType(String pageId, ChangeType changeType);

    /**
     * Find all publish events for a page
     */
    @Query("{ 'pageId': ?0, 'changeType': 'PUBLISH', 'deleted': { $ne: true } }")
    List<PageVersion> findPublishEvents(String pageId, Pageable pageable);

    /**
     * Find rollback events
     */
    @Query("{ 'pageId': ?0, 'changeType': 'ROLLBACK', 'deleted': { $ne: true } }")
    List<PageVersion> findRollbackEvents(String pageId);

    // =========================================================================
    // Version Statistics
    // =========================================================================

    /**
     * Get version statistics for a page
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'pageId': ?0, 'deleted': { $ne: true } } }",
            "{ '$group': { " +
                    "  '_id': null, " +
                    "  'totalVersions': { '$sum': 1 }, " +
                    "  'minVersionNumber': { '$min': '$versionNumber' }, " +
                    "  'maxVersionNumber': { '$max': '$versionNumber' }, " +
                    "  'firstVersionDate': { '$min': '$createdAt' }, " +
                    "  'lastVersionDate': { '$max': '$createdAt' }, " +
                    "  'publishCount': { '$sum': { '$cond': [ { '$eq': ['$changeType', 'PUBLISH'] }, 1, 0 ] } }, " +
                    "  'rollbackCount': { '$sum': { '$cond': [ { '$eq': ['$changeType', 'ROLLBACK'] }, 1, 0 ] } } " +
                    "} }"
    })
    Optional<VersionStatistics> getVersionStatistics(String pageId);

    /**
     * Count total versions for a page
     * Used for version pruning and storage optimization
     *
     * @param pageId the page identifier
     * @return total number of versions for the page
     */
    @Query(value = "{ 'pageId': ?0, 'deleted': { $ne: true } }", count = true)
    long countVersionsByPageId(String pageId);

    /**
     * Count versions by change type
     * Used for analytics and reporting
     *
     * @param pageId the page identifier
     * @param changeType the change type to filter by
     * @return number of versions with the specified change type
     */
    @Query(value = "{ 'pageId': ?0, 'changeType': ?1, 'deleted': { $ne: true } }", count = true)
    long countVersionsByChangeType(String pageId, ChangeType changeType);

    /**
     * Count versions created after a specific date
     * Used for retention policy enforcement
     *
     * @param pageId the page identifier
     * @param since the date threshold
     * @return number of versions created after the specified date
     */
    @Query(value = "{ 'pageId': ?0, 'createdAt': { $gt: ?1 }, 'deleted': { $ne: true } }", count = true)
    long countVersionsSince(String pageId, Instant since);

    /**
     * Count archived versions for a page
     * Used for archival management
     *
     * @param pageId the page identifier
     * @return number of archived versions
     */
    @Query(value = "{ 'pageId': ?0, 'archived': true, 'deleted': { $ne: true } }", count = true)
    long countArchivedVersions(String pageId);

    // =========================================================================
    // Version Cleanup Operations
    // =========================================================================

    /**
     * Find old versions for pruning
     */
    @Query("{ 'pageId': ?0, 'createdAt': { $lt: ?1 }, 'isCurrent': false, 'isPublished': false, 'deleted': { $ne: true } }")
    List<PageVersion> findOldVersionsForPruning(String pageId, Instant cutoffDate);

    /**
     * Soft delete version
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'deleted': true, 'deletedAt': ?1, 'deletedBy': ?2 } }")
    void softDeleteVersion(String versionId, Instant deletedAt, String deletedBy);

    /**
     * Archive version (soft delete with archival flag)
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'archived': true, 'archivedAt': ?1, 'archivedBy': ?2 } }")
    void archiveVersion(String versionId, Instant archivedAt, String archivedBy);

    /**
     * Restore archived version
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'archived': false, 'archivedAt': null, 'archivedBy': null } }")
    void restoreVersion(String versionId);

    // =========================================================================
    // Version Tag Operations
    // =========================================================================

    /**
     * Find versions by tag
     */
    @Query("{ 'pageId': ?0, 'tags': ?1, 'deleted': { $ne: true } }")
    List<PageVersion> findByPageIdAndTag(String pageId, String tag);

    /**
     * Find versions by multiple tags (AND condition)
     */
    @Query("{ 'pageId': ?0, 'tags': { $all: ?1 }, 'deleted': { $ne: true } }")
    List<PageVersion> findByPageIdAndTags(String pageId, Set<String> tags);

    /**
     * Add tag to version
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$addToSet': { 'tags': ?1 }, '$set': { 'lastModifiedDate': ?2 } }")
    void addTagToVersion(String versionId, String tag, Instant modifiedDate);

    /**
     * Remove tag from version
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'tags': ?1 }, '$set': { 'lastModifiedDate': ?2 } }")
    void removeTagFromVersion(String versionId, String tag, Instant modifiedDate);

    // =========================================================================
    // Version Search & Filtering
    // =========================================================================

    /**
     * Find versions by author
     */
    @Query("{ 'pageId': ?0, 'createdBy': ?1, 'deleted': { $ne: true } }")
    List<PageVersion> findByPageIdAndAuthor(String pageId, String author, Pageable pageable);

    /**
     * Find versions by branch and date range
     */
    @Query("{ 'pageId': ?0, 'branchName': ?1, 'createdAt': { $gte: ?2, $lte: ?3 }, 'deleted': { $ne: true } }")
    List<PageVersion> findByPageIdAndBranchAndDateRange(String pageId, String branchName, Instant start, Instant end);

    /**
     * Find versions that are not merged
     */
    @Query("{ 'pageId': ?0, 'isMerged': false, 'deleted': { $ne: true } }")
    List<PageVersion> findUnmergedVersions(String pageId);
}

// =========================================================================
// DTO Classes
// =========================================================================

/**
 * Branch summary for dashboard
 */
class BranchSummary {
    private String branchName;
    private Integer latestVersion;
    private String latestVersionId;
    private Instant lastModified;

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public Integer getLatestVersion() { return latestVersion; }
    public void setLatestVersion(Integer latestVersion) { this.latestVersion = latestVersion; }
    public String getLatestVersionId() { return latestVersionId; }
    public void setLatestVersionId(String latestVersionId) { this.latestVersionId = latestVersionId; }
    public Instant getLastModified() { return lastModified; }
    public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
}

/**
 * Version statistics aggregation
 */
class VersionStatistics {
    private long totalVersions;
    private Integer minVersionNumber;
    private Integer maxVersionNumber;
    private Instant firstVersionDate;
    private Instant lastVersionDate;
    private long publishCount;
    private long rollbackCount;

    public long getTotalVersions() { return totalVersions; }
    public void setTotalVersions(long totalVersions) { this.totalVersions = totalVersions; }
    public Integer getMinVersionNumber() { return minVersionNumber; }
    public void setMinVersionNumber(Integer minVersionNumber) { this.minVersionNumber = minVersionNumber; }
    public Integer getMaxVersionNumber() { return maxVersionNumber; }
    public void setMaxVersionNumber(Integer maxVersionNumber) { this.maxVersionNumber = maxVersionNumber; }
    public Instant getFirstVersionDate() { return firstVersionDate; }
    public void setFirstVersionDate(Instant firstVersionDate) { this.firstVersionDate = firstVersionDate; }
    public Instant getLastVersionDate() { return lastVersionDate; }
    public void setLastVersionDate(Instant lastVersionDate) { this.lastVersionDate = lastVersionDate; }
    public long getPublishCount() { return publishCount; }
    public void setPublishCount(long publishCount) { this.publishCount = publishCount; }
    public long getRollbackCount() { return rollbackCount; }
    public void setRollbackCount(long rollbackCount) { this.rollbackCount = rollbackCount; }
}