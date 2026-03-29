package com.purehome.uicore.dto.response;

import com.purehome.uicore.model.PageAuditEvent.EventType;
import com.purehome.uicore.model.PageAuditEvent.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * FAANG-GRADE AUDIT ANALYTICS RESPONSE DTO
 *
 * Comprehensive audit analytics with trends and patterns
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditAnalyticsResponse {

    private Instant startDate;
    private Instant endDate;
    private String groupBy;
    private long totalEvents;
    private Map<String, Long> eventsByType;
    private Map<Severity, Long> eventsBySeverity;
    private Map<String, Object> trends;

    private List<TimeSeriesPoint> timeSeries;

    @Data
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private Instant timestamp;
        private long count;
        private Map<EventType, Long> breakdown;
    }
}