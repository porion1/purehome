package com.purehome.uicore.scheduler;

import com.purehome.uicore.model.Page;
import com.purehome.uicore.model.PageStatus;
import com.purehome.uicore.repository.PageRepository;
import com.purehome.uicore.service.PagePublishService;
import com.purehome.uicore.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * FAANG-GRADE PUBLISH SCHEDULER
 *
 * Handles scheduled publishing and unpublishing of pages
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublishScheduler {

    private final PageRepository pageRepository;
    private final PagePublishService publishService;
    private final WebSocketService webSocketService;

    /**
     * Check for pages scheduled to be published
     * Runs every minute
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processScheduledPublishes() {
        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minusSeconds(60);

        List<Page> pagesToPublish = pageRepository.findPagesScheduledForPublish(oneMinuteAgo, now);

        if (!pagesToPublish.isEmpty()) {
            log.info("Found {} pages scheduled for publishing", pagesToPublish.size());

            for (Page page : pagesToPublish) {
                try {
                    log.info("Executing scheduled publish for page: {} (scheduled at: {})",
                            page.getId(), page.getScheduledPublishDate());

                    publishService.publishPage(page.getId(), "SYSTEM", null, "UTC",
                            PagePublishService.PublishOptions.defaultOptions());

                } catch (Exception e) {
                    log.error("Failed to publish scheduled page: {}", page.getId(), e);
                }
            }
        }
    }

    /**
     * Check for pages scheduled to be unpublished
     * Runs every minute
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processScheduledUnpublishes() {
        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minusSeconds(60);

        List<Page> pagesToUnpublish = pageRepository.findPagesScheduledForUnpublish(oneMinuteAgo, now);

        if (!pagesToUnpublish.isEmpty()) {
            log.info("Found {} pages scheduled for unpublishing", pagesToUnpublish.size());

            for (Page page : pagesToUnpublish) {
                try {
                    log.info("Executing scheduled unpublish for page: {} (scheduled at: {})",
                            page.getId(), page.getScheduledUnpublishDate());

                    publishService.unpublishPage(page.getId(), "SYSTEM", null, "Scheduled unpublish");

                } catch (Exception e) {
                    log.error("Failed to unpublish scheduled page: {}", page.getId(), e);
                }
            }
        }
    }
}