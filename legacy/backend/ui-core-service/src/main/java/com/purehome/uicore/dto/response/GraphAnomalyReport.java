package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Graph anomaly report response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphAnomalyReport {
    private boolean hasAnomalies;
    private double anomalyScore;
    private List<String> anomalies;
    private List<String> suspiciousPatterns;
    private List<String> recommendations;
}