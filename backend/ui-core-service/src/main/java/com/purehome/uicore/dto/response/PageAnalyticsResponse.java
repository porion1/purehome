package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * FAANG-GRADE PAGE ANALYTICS RESPONSE DTO
 *
 * Comprehensive page analytics with performance insights
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageAnalyticsResponse {

    private String pageId;
    private long totalViews;
    private long uniqueVisitors;
    private double avgTimeOnPageSeconds;
    private double bounceRate;
    private Double performanceScore;
    private Double seoScore;

    private Map<String, Long> viewsByDate;
    private List<String> topReferrers;
    private Map<String, Long> deviceBreakdown;
    private Map<String, Long> browserBreakdown;
    private Map<String, Long> countryBreakdown;

    private List<String> recommendations;

    public double getEngagementScore() {
        return (1.0 - bounceRate) * (avgTimeOnPageSeconds / 60.0) * (uniqueVisitors / (double) Math.max(1, totalViews));
    }
}