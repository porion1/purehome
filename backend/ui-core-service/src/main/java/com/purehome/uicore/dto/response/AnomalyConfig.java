package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Anomaly detection configuration response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyConfig {
    private double zScoreThreshold;
    private int windowSizeMinutes;
    private boolean useExponentialMovingAverage;
    private double sensitivityMultiplier;
    private List<String> ignoredPatterns;
    private Map<String, Double> userOverrides;
}