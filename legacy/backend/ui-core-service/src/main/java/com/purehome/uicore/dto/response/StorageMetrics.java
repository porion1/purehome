package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Storage metrics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageMetrics {
    private long totalBytes;
    private Map<String, Long> bytesByTier;
    private Map<String, Long> bytesByMonth;
    private double projectedGrowthRate;
    private long projectedBytesNextMonth;
    private List<String> optimizationRecommendations;
}