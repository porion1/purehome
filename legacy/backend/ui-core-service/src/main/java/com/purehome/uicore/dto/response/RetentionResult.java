package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Retention result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionResult {
    private int eventsMoved;
    private int eventsDeleted;
    private long spaceFreedBytes;
    private Map<String, Integer> eventsByTier;
    private List<String> warnings;
}