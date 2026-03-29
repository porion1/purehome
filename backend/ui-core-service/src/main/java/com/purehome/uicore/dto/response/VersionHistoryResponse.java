package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Version history response with pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionHistoryResponse {
    private List<VersionResponse> versions;
    private String nextCursor;
    private String previousCursor;
    private boolean hasNext;
    private boolean hasPrevious;
    private int totalCount;
}