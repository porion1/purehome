package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * User behavior analytics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorAnalytics {
    private int totalActiveUsers;
    private int newUsers;
    private int returningUsers;
    private double engagementScore;
    private List<UserActivitySummary> topActiveUsers;
    private Map<String, Integer> actionsByType;

    @Data
    @AllArgsConstructor
    public static class UserActivitySummary {
        private String userId;
        private int eventCount;
        private int uniquePages;
        private Instant lastActive;
        private double activityScore;
    }
}