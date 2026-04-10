package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Merge base result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeBaseResult {
    private VersionGraphResponse.GraphNode mergeBase;
    private double confidence;
    private int distanceToSource;
    private int distanceToTarget;
    private String recommendation;
}