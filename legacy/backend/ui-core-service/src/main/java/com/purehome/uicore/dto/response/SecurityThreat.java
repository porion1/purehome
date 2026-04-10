package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Security threat response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityThreat {
    private String threatId;
    private String threatType;
    private String severity;
    private List<String> indicators;
    private List<AuditEventResponse> evidence;
    private String description;
    private List<String> mitigationSteps;
}