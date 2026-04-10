package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chain compression result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainCompressionResult {
    private int chainsCompressed;
    private int nodesCompressed;
    private long originalNodes;
    private long compressedNodes;
    private long spaceSavedBytes;
    private List<String> compressedChainIds;
}