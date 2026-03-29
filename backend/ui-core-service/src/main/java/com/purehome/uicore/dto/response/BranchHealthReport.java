package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Branch health report response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchHealthReport {
    private String branchName;
    private double healthScore;
    private String healthStatus;
    private int commitCount;
    private double activityScore;
    private double stabilityScore;
    private List<String> recommendations;
    private boolean isStale;
}