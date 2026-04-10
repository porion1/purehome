package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Insider threat detection response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsiderThreat {
    private String userId;
    private double riskScore;
    private List<String> suspiciousActivities;
    private List<String> accessedResources;
    private int unusualAccessCount;
    private String riskLevel;
}