package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Branch comparison response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchComparison {
    private String sourceBranch;
    private String targetBranch;
    private int sourceCommits;
    private int targetCommits;
    private int divergedCommits;
    private double divergenceScore;
    private List<String> uniqueToSource;
    private List<String> uniqueToTarget;
    private Optional<VersionResponse> commonAncestor;
}