package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * FAANG-GRADE COMPLIANCE REPORT RESPONSE DTO
 *
 * GDPR, CCPA, SOC2 compliance report data
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReportResponse {

    private String userId;
    private Instant reportPeriodStart;
    private Instant reportPeriodEnd;
    private List<Map<String, Object>> data;
    private boolean anonymized;
    private String format;
    private Instant generatedAt;

    private String reportId;
    private String downloadUrl;
    private Instant expiresAt;
}