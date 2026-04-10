package com.purehome.uicore.service;

import com.purehome.uicore.dto.response.PageResponse;
import com.purehome.uicore.dto.response.AuditEventResponse;
import com.purehome.uicore.dto.response.VersionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * FAANG-GRADE WEBSOCKET SERVICE
 *
 * Handles real-time notifications for page updates, version changes, and audit events
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify subscribers about page updates
     */
    public void notifyPageUpdate(String pageId, String eventType, PageResponse page) {
        String destination = "/topic/pages/" + pageId;
        String workspaceDestination = "/topic/workspaces/" + page.getWorkspaceId();

        PageUpdateMessage message = new PageUpdateMessage(pageId, eventType, page, System.currentTimeMillis());

        // Send to page-specific subscribers
        messagingTemplate.convertAndSend(destination, message);

        // Send to workspace subscribers
        messagingTemplate.convertAndSend(workspaceDestination, message);

        log.debug("Page update notification sent - Page: {}, Event: {}", pageId, eventType);
    }

    /**
     * Notify user about their specific page updates
     */
    public void notifyUserPageUpdate(String userId, String pageId, String eventType, PageResponse page) {
        String destination = "/user/" + userId + "/topic/pages";

        PageUpdateMessage message = new PageUpdateMessage(pageId, eventType, page, System.currentTimeMillis());

        messagingTemplate.convertAndSendToUser(userId, "/topic/pages", message);

        log.debug("User-specific page update sent - User: {}, Page: {}, Event: {}", userId, pageId, eventType);
    }

    /**
     * Notify subscribers about version updates
     */
    public void notifyVersionUpdate(String pageId, VersionResponse version) {
        String destination = "/topic/versions/" + pageId;

        VersionUpdateMessage message = new VersionUpdateMessage(pageId, version, System.currentTimeMillis());

        messagingTemplate.convertAndSend(destination, message);

        log.debug("Version update notification sent - Page: {}, Version: {}", pageId, version.getVersionNumber());
    }

    /**
     * Notify subscribers about audit events
     */
    public void notifyAuditEvent(String workspaceId, AuditEventResponse event) {
        String destination = "/topic/audit/" + workspaceId;

        AuditEventMessage message = new AuditEventMessage(event, System.currentTimeMillis());

        messagingTemplate.convertAndSend(destination, message);

        log.debug("Audit event notification sent - Workspace: {}, Event: {}", workspaceId, event.getEventType());
    }

    /**
     * Notify about security alerts
     */
    public void notifySecurityAlert(String userId, SecurityAlertMessage alert) {
        String destination = "/user/" + userId + "/topic/alerts";

        messagingTemplate.convertAndSendToUser(userId, "/topic/alerts", alert);

        log.warn("Security alert sent to user: {} - {}", userId, alert.getTitle());
    }

    /**
     * Broadcast to all connected clients
     */
    public void broadcast(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Broadcast sent to: {}", destination);
    }

    // =========================================================================
    // Message DTOs
    // =========================================================================

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PageUpdateMessage {
        private String pageId;
        private String eventType;
        private PageResponse page;
        private long timestamp;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class VersionUpdateMessage {
        private String pageId;
        private VersionResponse version;
        private long timestamp;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AuditEventMessage {
        private AuditEventResponse event;
        private long timestamp;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SecurityAlertMessage {
        private String alertId;
        private String title;
        private String severity;
        private String description;
        private long timestamp;
    }
}