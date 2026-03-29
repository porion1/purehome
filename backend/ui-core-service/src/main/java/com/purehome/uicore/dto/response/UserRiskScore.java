package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * User risk score response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRiskScore {
    private String userId;
    private double overallRisk;
    private Map<String, Double> riskFactors;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> contributingEvents;
    private List<String> recommendations;
}