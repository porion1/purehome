package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Threat intelligence response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatIntelligence {
    private int totalThreats;
    private Map<String, Integer> threatsByType;
    private Map<String, Integer> threatsBySeverity;
    private List<String> emergingThreats;
    private List<String> trendingAttackVectors;
    private double threatLevel;
    private String recommendation;
}