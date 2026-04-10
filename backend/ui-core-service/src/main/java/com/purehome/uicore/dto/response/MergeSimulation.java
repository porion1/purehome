package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Merge simulation response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeSimulation {
    private boolean hasConflicts;
    private List<MergeConflict> conflicts;
    private List<String> autoResolvable;
    private VersionGraphResponse.GraphNode mergeBase;
    private MergeComplexityScore complexity;
    private List<String> warnings;

    @Data
    @AllArgsConstructor
    public static class MergeConflict {
        private String type;
        private Object leftValue;
        private Object rightValue;
        private String resolution;
    }
}