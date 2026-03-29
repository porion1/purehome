package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data deletion result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDeletionResult {
    private boolean success;
    private String message;
    private int recordsDeleted;
    private int recordsAnonymized;
    private List<String> retainedDataReasons;
    private String requestId;
}