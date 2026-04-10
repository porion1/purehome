package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FAANG-GRADE VERSION GRAPH RESPONSE DTO
 *
 * Complete version graph representation with nodes and edges
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionGraphResponse {

    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    private int totalNodes;
    private int totalEdges;
    private boolean hasCycles;
    private List<List<String>> cycles;

    /**
     * Graph node representing a version
     */
    @Data
    @AllArgsConstructor
    public static class GraphNode {
        private String id;
        private String versionNumber;
        private String versionString;
        private String branchName;
        private Instant createdAt;
        private String createdBy;
        private String changeType;
        private Set<String> parentIds;
        private Set<String> childIds;
        private int depth;
        private double influenceScore;
        private String merkleHash;
        private boolean isHead;
        private boolean isRoot;
    }

    /**
     * Graph edge representing relationship between versions
     */
    @Data
    @AllArgsConstructor
    public static class GraphEdge {
        private String source;
        private String target;
        private String type; // PARENT, CHILD, MERGE
    }
}