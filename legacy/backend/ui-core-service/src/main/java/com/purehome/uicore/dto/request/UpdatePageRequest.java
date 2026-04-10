package com.purehome.uicore.dto.request;

import com.purehome.uicore.model.PageLayout;
import com.purehome.uicore.model.PageMetadata;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

/**
 * FAANG-GRADE UPDATE PAGE REQUEST DTO
 *
 * Supports partial updates with intelligent field merging
 * Uses optimistic locking for concurrent updates
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePageRequest {

    // =========================================================================
    // Updatable Fields
    // =========================================================================

    private String title;

    private String description;

    private String keywords;

    @Valid
    private PageMetadata metadata;

    @Valid
    private PageLayout layout;

    private String parentPageId;

    private Set<String> tags;

    private Map<String, Object> customAttributes;

    // =========================================================================
    // Visibility Updates
    // =========================================================================

    private String visibility;

    private Boolean requiresAuth;

    private Set<String> allowedRoles;

    private String passwordHash;

    // =========================================================================
    // SEO Updates
    // =========================================================================

    private String seoTitle;

    private String seoDescription;

    private String canonicalUrl;

    private Boolean indexable;

    private Boolean followLinks;

    // =========================================================================
    // Open Graph Updates
    // =========================================================================

    private String ogTitle;
    private String ogDescription;
    private String ogImage;
    private String ogType;

    // =========================================================================
    // Twitter Card Updates
    // =========================================================================

    private String twitterCard;
    private String twitterTitle;
    private String twitterDescription;
    private String twitterImage;

    // =========================================================================
    // Optimistic Locking
    // =========================================================================

    private Long expectedVersion;

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Checks if any fields are being updated
     */
    public boolean hasChanges() {
        return title != null ||
                description != null ||
                keywords != null ||
                metadata != null ||
                layout != null ||
                parentPageId != null ||
                tags != null ||
                customAttributes != null ||
                visibility != null ||
                requiresAuth != null ||
                allowedRoles != null ||
                passwordHash != null ||
                seoTitle != null ||
                seoDescription != null ||
                canonicalUrl != null ||
                indexable != null ||
                followLinks != null ||
                ogTitle != null ||
                ogDescription != null ||
                ogImage != null ||
                ogType != null ||
                twitterCard != null ||
                twitterTitle != null ||
                twitterDescription != null ||
                twitterImage != null;
    }
}