package com.purehome.uicore.scheduler;

import com.purehome.uicore.service.PageAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FAANG-GRADE AUDIT RETENTION SCHEDULER
 *
 * Automatically applies retention policies to audit logs
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRetentionScheduler {

    private final PageAuditService auditService;

    /**
     * Apply retention policy daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void applyRetentionPolicy() {
        log.info("Starting scheduled audit retention policy application");

        long startTime = System.currentTimeMillis();

        try {
            PageAuditService.RetentionResult result = auditService.applyRetentionPolicy(null, false);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Audit retention completed - Events moved: {}, Events deleted: {}, Space freed: {} bytes, Duration: {}ms",
                    result.getEventsMoved(), result.getEventsDeleted(), result.getSpaceFreedBytes(), duration);

        } catch (Exception e) {
            log.error("Audit retention failed", e);
        }
    }

    /**
     * Archive old audit data monthly on the 1st at 4 AM
     */
    @Scheduled(cron = "0 0 4 1 * *")
    public void archiveOldAuditData() {
        log.info("Starting scheduled audit archival");

        long startTime = System.currentTimeMillis();

        try {
            PageAuditService.ArchiveResult result = auditService.archiveAuditData(365, null);

            long duration = System.currentTimeMillis() - startTime;
            // Fixed: Use getArchivedBytes() instead of getArchivedSizeBytes()
            log.info("Audit archival completed - Events archived: {}, Archived size: {} bytes, Duration: {}ms",
                    result.getEventsArchived(), result.getArchivedBytes(), duration);

        } catch (Exception e) {
            log.error("Audit archival failed", e);
        }
    }
}