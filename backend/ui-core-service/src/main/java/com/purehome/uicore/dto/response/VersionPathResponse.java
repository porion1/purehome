package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Version path response for graph traversal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionPathResponse {
    private List<VersionGraphResponse.GraphNode> nodes;
    private int pathLength;
    private boolean connected;
}