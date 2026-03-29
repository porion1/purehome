package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * FAANG-GRADE VERSION DIFF RESPONSE DTO
 *
 * Comprehensive diff between two versions with multiple format support
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionDiffResponse {

    private String sourceVersionId;
    private String targetVersionId;
    private Integer sourceVersionNumber;
    private Integer targetVersionNumber;

    private Map<String, DiffChange> changes;
    private String diffText;
    private int totalChanges;

    /**
     * Diff change detail
     */
    @Data
    @AllArgsConstructor
    public static class DiffChange {
        private Object oldValue;
        private Object newValue;
        private String type; // ADDED, REMOVED, MODIFIED
    }
}