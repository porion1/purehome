package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FAANG-GRADE PAGE CURSOR RESPONSE DTO
 *
 * Keyset pagination response with next/previous cursors
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageCursorResponse {

    private List<PageResponse> pages;
    private String nextCursor;
    private String previousCursor;
    private boolean hasNext;
    private boolean hasPrevious;
    private long totalCount;

    public static PageCursorResponse empty() {
        return new PageCursorResponse(List.of(), null, null, false, false, 0);
    }
}