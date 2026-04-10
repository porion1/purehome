// This should be in the appropriate DTO package
package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrityReport {
    private boolean valid;
    private double integrityScore;
    private List<String> corruptedVersions;
    private List<String> brokenLinks;
    private List<String> missingReferences;
    private List<String> repairedNodes;
    private String repairRecommendation;
}