package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Graph evolution response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEvolution {
    private List<EvolutionPoint> points;
    private Map<String, Integer> growthByPeriod;
    private List<String> activitySpikes;
    private double growthRate;

    @Data
    @AllArgsConstructor
    public static class EvolutionPoint {
        private Instant timestamp;
        private int nodeCount;
        private int edgeCount;
        private int branchCount;
        private double complexity;
    }
}