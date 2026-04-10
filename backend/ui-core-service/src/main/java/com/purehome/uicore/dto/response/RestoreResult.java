package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Restore result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreResult {
    private String restoreId;
    private String downloadUrl;
    private Instant expiresAt;
    private int eventsRestored;
    private long restoredBytes;
}