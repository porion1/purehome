package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Graph centrality metrics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphCentrality {
    private Map<String, Double> degreeCentrality;
    private Map<String, Double> betweennessCentrality;
    private Map<String, Double> closenessCentrality;
    private Map<String, Double> eigenvectorCentrality;
    private String mostCentralNode;
}