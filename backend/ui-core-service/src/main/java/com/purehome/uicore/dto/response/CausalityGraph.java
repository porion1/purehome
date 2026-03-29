package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Causality graph response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CausalityGraph {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    private int totalNodes;
    private int totalEdges;
    private boolean hasCycles;

    @Data
    @AllArgsConstructor
    public static class GraphNode {
        private String id;
        private String type;
        private Instant timestamp;
        private Map<String, Object> properties;
    }

    @Data
    @AllArgsConstructor
    public static class GraphEdge {
        private String source;
        private String target;
        private String relationship;
        private double weight;
    }
}