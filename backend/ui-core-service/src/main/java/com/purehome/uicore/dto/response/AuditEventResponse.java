package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * FAANG-GRADE AUDIT EVENT RESPONSE DTO
 *
 * Comprehensive audit event with anomaly detection data
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventResponse {

    private String eventId;
    private String pageId;
    private String eventType;
    private String severity;
    private String userId;
    private String action;
    private String description;
    private Instant timestamp;
    private Boolean success;
    private String correlationId;
    private Boolean flagged;
    private Double anomalyScore;
    private Map<String, Object> changes;

    public boolean isAnomaly() {
        return Boolean.TRUE.equals(flagged) || (anomalyScore != null && anomalyScore > 70);
    }
}