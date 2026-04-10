package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Prune result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PruneResult {
    private int nodesPruned;
    private long spaceFreedBytes;
    private List<String> prunedNodeIds;
    private String archiveId;
}