package com.purehome.uicore.dto.mapper;

import com.purehome.uicore.dto.response.AuditEventResponse;
import com.purehome.uicore.model.PageAuditEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * FAANG-GRADE AUDIT MAPPER
 *
 * Maps AuditEvent entities to AuditEventResponse DTO
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Mapper(componentModel = "spring")
public interface AuditMapper {

    AuditMapper INSTANCE = Mappers.getMapper(AuditMapper.class);

    /**
     * Convert PageAuditEvent to AuditEventResponse
     */
    @Mapping(source = "id", target = "eventId")
    @Mapping(source = "eventType", target = "eventType", qualifiedByName = "eventTypeToString")
    @Mapping(source = "severity", target = "severity", qualifiedByName = "severityToString")
    AuditEventResponse toResponse(PageAuditEvent event);

    /**
     * Convert EventType enum to String
     */
    @Named("eventTypeToString")
    default String eventTypeToString(PageAuditEvent.EventType eventType) {
        return eventType != null ? eventType.getCode() : null;
    }

    /**
     * Convert Severity enum to String
     */
    @Named("severityToString")
    default String severityToString(PageAuditEvent.Severity severity) {
        return severity != null ? severity.getDisplayName() : null;
    }
}