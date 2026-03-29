package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Archive result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveResult {
    private int versionsArchived;
    private long archivedSizeBytes;
    private List<Integer> archivedVersionNumbers;
}