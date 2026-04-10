package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * FAANG-GRADE PUBLISH STATUS RESPONSE DTO
 *
 * Comprehensive publish status tracking with real-time updates
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishStatusResponse {

    private String pageId;
    private String jobId;
    private boolean success;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED, SCHEDULED
    private Integer newVersion;
    private List<String> actions;
    private String errorMessage;
    private Instant scheduledTime;
    private Instant completedAt;
    private long durationMs;

    public static PublishStatusResponse success(String pageId, Integer newVersion, List<String> actions, long durationMs) {
        return PublishStatusResponse.builder()
                .pageId(pageId)
                .success(true)
                .status("COMPLETED")
                .newVersion(newVersion)
                .actions(actions)
                .completedAt(Instant.now())
                .durationMs(durationMs)
                .build();
    }

    public static PublishStatusResponse scheduled(String pageId, String jobId, Instant scheduledTime) {
        return PublishStatusResponse.builder()
                .pageId(pageId)
                .jobId(jobId)
                .success(true)
                .status("SCHEDULED")
                .scheduledTime(scheduledTime)
                .build();
    }

    public static PublishStatusResponse failure(String pageId, String errorMessage) {
        return PublishStatusResponse.builder()
                .pageId(pageId)
                .success(false)
                .status("FAILED")
                .errorMessage(errorMessage)
                .completedAt(Instant.now())
                .build();
    }

    public static PublishStatusResponse processing(String pageId, String jobId) {
        return PublishStatusResponse.builder()
                .pageId(pageId)
                .jobId(jobId)
                .success(true)
                .status("PROCESSING")
                .build();
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isScheduled() {
        return "SCHEDULED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}