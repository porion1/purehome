package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * User activity timeline response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityTimeline {
    private String userId;
    private List<UserSession> sessions;
    private ActivitySummary summary;
    private Instant startTime;
    private Instant endTime;

    @Data
    @AllArgsConstructor
    public static class ActivitySummary {
        private int totalEvents;
        private int uniquePages;
        private Map<String, Integer> eventCounts;
        private Map<String, Integer> severityCounts;
        private double eventsPerHour;
    }
}