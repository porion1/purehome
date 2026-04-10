package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Event chain response for correlation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventChain {
    private String correlationId;
    private AuditEventResponse rootEvent;
    private List<EventChain> children;
    private int depth;
    private long totalDurationMs;
    private boolean hasCycle;
}