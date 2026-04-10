package com.purehome.uicore.dto.request;

import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * FAANG-GRADE PUBLISH PAGE REQUEST DTO
 *
 * Supports immediate and scheduled publishing
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishPageRequest {

    @Future(message = "Publish time must be in the future")
    private Instant publishTime;

    private String timezone;

    private boolean immediate;

    private String publishMessage;

    private boolean notifySubscribers;

    private boolean invalidateCache;

    private boolean updateSitemap;

    private boolean pingSearchEngines;
}