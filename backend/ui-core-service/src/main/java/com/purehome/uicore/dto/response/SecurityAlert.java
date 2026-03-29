package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Security alert response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlert {
    private String alertId;
    private String title;
    private String description;
    private String severity;
    private double confidence;
    private Instant detectedAt;
    private String detectedBy;
    private List<String> evidence;
    private List<String> remediationSteps;
    private String status; // OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE
}