package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Export status response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportStatus {
    private String exportId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private int progressPercent;
    private String downloadUrl;
    private Instant expiresAt;
    private String errorMessage;

    public boolean isComplete() {
        return "COMPLETED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}