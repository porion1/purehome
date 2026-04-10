package com.purehome.uicore.dto.response;

import com.purehome.uicore.model.PageAuditEvent.StorageTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsageReport {
    private StorageTier storageTier;
    private long count;
    private Instant oldestEvent;
    private Instant newestEvent;
}