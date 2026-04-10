package com.purehome.uicore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FAANG-GRADE ROLLBACK VERSION REQUEST DTO
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackVersionRequest {

    @Min(value = 1, message = "Target version must be at least 1")
    private Integer targetVersion;

    @NotBlank(message = "Rollback reason is required")
    private String reason;

    private boolean createSnapshot;

    private boolean preserveCurrentVersion;
}