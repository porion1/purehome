package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * FAANG-GRADE VERSION RESPONSE DTO
 *
 * Comprehensive version information with integrity data
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {

    // =========================================================================
    // Core Version Fields
    // =========================================================================

    private String versionId;
    private String pageId;
    private Integer versionNumber;
    private String versionString;
    private String parentVersionId;
    private String branchName;

    // =========================================================================
    // Content Snapshot
    // =========================================================================

    private PageResponse pageSnapshot;
    private Map<String, Object> changes;
    private String diff;

    // =========================================================================
    // Version Metadata
    // =========================================================================

    private Instant createdAt;
    private String createdBy;
    private String changeDescription;
    private String changeType;
    private Set<String> tags;
    private Map<String, Object> metadata;
    private Boolean isCurrent;
    private Boolean isPublished;
    private Instant publishedAt;
    private String publishedBy;

    // =========================================================================
    // Integrity
    // =========================================================================

    private String merkleHash;
    private String commitHash;

    // =========================================================================
    // Helper Methods
    // =========================================================================

    public boolean isMajorVersion() {
        return versionString != null && versionString.matches("\\d+\\.0\\.0");
    }

    public boolean isMinorVersion() {
        return versionString != null && versionString.matches("\\d+\\.\\d+\\.0");
    }

    public boolean isPatchVersion() {
        return versionString != null && versionString.matches("\\d+\\.\\d+\\.\\d+") && !isMajorVersion() && !isMinorVersion();
    }

    public Map<String, Object> toVersionInfo() {
        return Map.of(
                "versionId", versionId,
                "versionNumber", versionNumber,
                "versionString", versionString,
                "changeType", changeType,
                "changeDescription", changeDescription,
                "createdAt", createdAt,
                "createdBy", createdBy,
                "isCurrent", isCurrent,
                "isPublished", isPublished
        );
    }
}