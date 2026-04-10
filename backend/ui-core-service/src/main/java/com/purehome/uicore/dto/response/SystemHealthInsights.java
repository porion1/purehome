package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * System health insights response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthInsights {
    private double errorRate;
    private double averageResponseTimeMs;
    private int totalFailures;
    private List<String> topFailureTypes;
    private List<String> recommendations;
    private String healthStatus;
}