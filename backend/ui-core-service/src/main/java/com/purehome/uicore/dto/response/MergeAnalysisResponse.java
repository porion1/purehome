package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FAANG-GRADE MERGE ANALYSIS RESPONSE DTO
 *
 * Detailed merge analysis with conflict detection and resolution suggestions
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeAnalysisResponse {

    private boolean success;
    private String mergedVersionId;
    private List<String> conflicts;
    private List<String> autoResolvable;
    private List<String> manualResolutionNeeded;
    private long estimatedTimeMs;

    public boolean hasConflicts() {
        return conflicts != null && !conflicts.isEmpty();
    }

    public boolean canAutoResolve() {
        return autoResolvable != null && autoResolvable.size() == (conflicts != null ? conflicts.size() : 0);
    }
}