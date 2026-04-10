package com.purehome.uicore.dto.response;

import com.purehome.uicore.model.PageLayout;
import com.purehome.uicore.model.PageMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * FAANG-GRADE PAGE RESPONSE DTO
 *
 * Comprehensive page response with all metadata
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse {

    // =========================================================================
    // Core Fields
    // =========================================================================

    private String id;
    private String title;
    private String slug;
    private String status;
    private String description;
    private String keywords;

    // =========================================================================
    // Content Fields
    // =========================================================================

    private PageMetadata metadata;
    private PageLayout layout;

    // =========================================================================
    // Organizational Fields
    // =========================================================================

    private String workspaceId;
    private String siteId;
    private String parentPageId;
    private Set<String> childPages;

    // =========================================================================
    // Version Control
    // =========================================================================

    private Integer version;
    private String currentVersionId;
    private String publishedVersionId;

    // =========================================================================
    // Audit Fields
    // =========================================================================

    private String createdBy;
    private String lastModifiedBy;
    private Instant createdDate;
    private Instant lastModifiedDate;
    private Instant publishedDate;
    private String publishedBy;
    private Instant archivedDate;

    // =========================================================================
    // Scheduling
    // =========================================================================

    private Instant scheduledPublishDate;
    private Instant scheduledUnpublishDate;

    // =========================================================================
    // Security & Visibility
    // =========================================================================

    private Boolean requiresAuth;
    private Set<String> allowedRoles;
    private String visibility;

    // =========================================================================
    // Performance & SEO
    // =========================================================================

    private Double seoScore;
    private Double performanceScore;
    private Double accessibilityScore;
    private Long pageWeightBytes;
    private Integer estimatedLoadTimeMs;

    // =========================================================================
    // Metadata & Tagging
    // =========================================================================

    private Set<String> tags;
    private Map<String, Object> customAttributes;
    private Integer cacheTtlSeconds;

    // =========================================================================
    // Analytics
    // =========================================================================

    private Long viewCount;
    private Long uniqueVisitors;
    private Double avgTimeOnPageSeconds;
    private Double bounceRate;

    // =========================================================================
    // SEO Metadata
    // =========================================================================

    private String seoTitle;
    private String seoDescription;
    private String canonicalUrl;
    private Boolean indexable;
    private Boolean followLinks;

    // =========================================================================
    // Open Graph
    // =========================================================================

    private String ogTitle;
    private String ogDescription;
    private String ogImage;
    private String ogType;
    private String ogUrl;
    private String ogSiteName;

    // =========================================================================
    // Twitter Card
    // =========================================================================

    private String twitterCard;
    private String twitterSite;
    private String twitterCreator;
    private String twitterTitle;
    private String twitterDescription;
    private String twitterImage;

    // =========================================================================
    // Helper Methods
    // =========================================================================

    public boolean isPublished() {
        return "PUBLISHED".equals(status);
    }

    public boolean isDraft() {
        return "DRAFT".equals(status);
    }

    public boolean isScheduled() {
        return "SCHEDULED".equals(status);
    }

    public boolean isVisible() {
        return isPublished() &&
                (scheduledPublishDate == null || scheduledPublishDate.isBefore(Instant.now())) &&
                (scheduledUnpublishDate == null || scheduledUnpublishDate.isAfter(Instant.now()));
    }

    public Map<String, Object> toSummaryMap() {
        return Map.of(
                "id", id,
                "title", title,
                "slug", slug,
                "status", status,
                "publishedDate", publishedDate,
                "lastModifiedDate", lastModifiedDate,
                "viewCount", viewCount,
                "performanceScore", performanceScore,
                "seoScore", seoScore
        );
    }
}