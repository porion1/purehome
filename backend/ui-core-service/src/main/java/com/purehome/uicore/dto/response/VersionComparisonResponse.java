package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Version comparison response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionComparisonResponse {
    private String sourceVersionId;
    private String targetVersionId;
    private Map<String, Object> differences;
    private double similarityScore;
}