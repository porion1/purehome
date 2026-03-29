package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph repair result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphRepairResult {
    private boolean success;
    private int nodesRepaired;
    private int chainsRebuilt;
    private List<String> repairedNodes;
    private List<String> unrecoverableNodes;
    private String message;
}