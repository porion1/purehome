package com.purehome.uicore.dto.response;

import com.purehome.uicore.model.PageAuditEvent.EventType;
import com.purehome.uicore.model.PageAuditEvent.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSummary {
    private EventType eventType;
    private Severity severity;
    private long count;
    private int uniqueUserCount;
    private int uniquePageCount;
}