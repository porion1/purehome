package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * User session reconstruction response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private String sessionId;
    private Instant startTime;
    private Instant endTime;
    private long durationSeconds;
    private List<AuditEventResponse> events;
    private int eventCount;
    private Map<String, Integer> eventBreakdown;
}