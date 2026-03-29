package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph integrity report response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphIntegrityReport {
    private boolean valid;
    private double integrityScore;
    private List<String> corruptedNodes;
    private List<String> brokenChains;
    private List<String> missingReferences;
    private String repairRecommendation;
}