package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Root cause analysis response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RootCauseAnalysis {
    private String failureEventId;
    private String rootCauseEventId;
    private double confidence;
    private List<EventChain> evidenceChain;
    private String explanation;
    private List<String> recommendations;
}