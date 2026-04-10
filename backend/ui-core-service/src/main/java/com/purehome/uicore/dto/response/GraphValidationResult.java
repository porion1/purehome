package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph validation result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphValidationResult {
    private boolean isValid;
    private List<String> cycles;
    private List<String> orphanedNodes;
    private List<String> danglingReferences;
    private List<String> multipleParents;
    private List<String> warnings;
}