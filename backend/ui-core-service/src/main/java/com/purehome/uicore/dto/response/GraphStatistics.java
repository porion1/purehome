package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Graph statistics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphStatistics {
    private int nodeCount;
    private int edgeCount;
    private int branchCount;
    private double averageDegree;
    private double graphDensity;
    private int maxDepth;
    private double averageDepth;
    private int maxChildren;
    private double averageChildren;
    private String healthStatus;
}