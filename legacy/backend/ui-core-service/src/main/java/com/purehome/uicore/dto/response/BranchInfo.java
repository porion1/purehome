package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Branch information response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchInfo {
    private String name;
    private int headVersion;
    private int commitCount;
    private Instant lastCommit;
    private String lastAuthor;
    private boolean isMerged;
}