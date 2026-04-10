package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph optimization result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphOptimizationResult {
    private long originalSizeBytes;
    private long optimizedSizeBytes;
    private long savedBytes;
    private double compressionRatio;
    private int nodesCompressed;
    private int edgesRemoved;
    private List<String> optimizedChains;
}