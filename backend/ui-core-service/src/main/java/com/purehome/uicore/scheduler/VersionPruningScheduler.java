package com.purehome.uicore.scheduler;

import com.purehome.uicore.repository.PageRepository;
import com.purehome.uicore.repository.PageVersionRepository;
import com.purehome.uicore.service.PageVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FAANG-GRADE VERSION PRUNING SCHEDULER
 *
 * Automatically prunes old versions based on retention policy
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VersionPruningScheduler {

    private final PageRepository pageRepository;
    private final PageVersionService versionService;

    /**
     * Prune old versions daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void pruneOldVersions() {
        log.info("Starting scheduled version pruning");

        long startTime = System.currentTimeMillis();
        int totalPruned = 0;

        try {
            // Get all pages
            List<String> pageIds = pageRepository.findAll()
                    .stream()
                    .map(com.purehome.uicore.model.Page::getId)
                    .toList();

            for (String pageId : pageIds) {
                try {
                    PageVersionService.PruneResult result = versionService.pruneVersions(pageId, 365, "SYSTEM");
                    totalPruned += result.getVersionsRemoved();
                } catch (Exception e) {
                    log.error("Failed to prune versions for page: {}", pageId, e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Version pruning completed - Pruned: {} versions, Duration: {}ms", totalPruned, duration);

        } catch (Exception e) {
            log.error("Version pruning failed", e);
        }
    }
}