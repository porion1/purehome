package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Merge candidate response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCandidate {
    private String branchName;
    private VersionGraphResponse.GraphNode headVersion;
    private MergeComplexityScore complexity;
    private double priority;
    private String reason;
}