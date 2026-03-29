package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Merge complexity score response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeComplexityScore {
    private double complexity;
    private String level; // LOW, MEDIUM, HIGH, EXTREME
    private int estimatedConflicts;
    private long estimatedTimeMs;
    private List<String> riskFactors;
    private List<String> recommendations;
}