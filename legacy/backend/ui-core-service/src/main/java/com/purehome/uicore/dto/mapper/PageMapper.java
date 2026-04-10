package com.purehome.uicore.dto.mapper;

import com.purehome.uicore.dto.request.CreatePageRequest;
import com.purehome.uicore.dto.request.UpdatePageRequest;
import com.purehome.uicore.dto.response.PageResponse;
import com.purehome.uicore.model.Page;
import com.purehome.uicore.model.PageStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.Set;

/**
 * FAANG-GRADE PAGE MAPPER
 *
 * Intelligent mapping between entities and DTOs with custom transformations
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Mapper(componentModel = "spring")
public interface PageMapper {

    PageMapper INSTANCE = Mappers.getMapper(PageMapper.class);

    /**
     * Convert CreatePageRequest to Page entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "version", constant = "0")
    @Mapping(target = "viewCount", constant = "0L")
    @Mapping(target = "createdDate", expression = "java(java.time.Instant.now())")
    @Mapping(target = "lastModifiedDate", expression = "java(java.time.Instant.now())")
    @Mapping(target = "childPages", ignore = true)
    @Mapping(target = "versionTree", ignore = true)
    @Mapping(target = "optimisticLockVersion", ignore = true)
    Page toEntity(CreatePageRequest request);

    /**
     * Update Page entity from UpdatePageRequest
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "version", expression = "java(page.getVersion() + 1)")
    @Mapping(target = "lastModifiedDate", expression = "java(java.time.Instant.now())")
    @Mapping(target = "childPages", ignore = true)
    @Mapping(target = "versionTree", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "optimisticLockVersion", ignore = true)
    void updateEntity(UpdatePageRequest request, @MappingTarget Page page);

    /**
     * Convert Page entity to PageResponse DTO
     */
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    @Mapping(source = "parentPageId", target = "parentPageId")
    @Mapping(source = "childPages", target = "childPages")
    PageResponse toResponse(Page page);

    /**
     * Convert PageStatus to String
     */
    @Named("statusToString")
    default String statusToString(PageStatus status) {
        return status != null ? status.getValue() : null;
    }

    /**
     * Convert String to PageStatus
     */
    default PageStatus stringToStatus(String status) {
        return status != null ? PageStatus.fromValue(status) : null;
    }

    /**
     * Enrich page with metadata from request
     */
    default Page enrichWithMetadata(Page page, CreatePageRequest request) {
        if (request.getMetadata() != null) {
            page.setMetadata(request.getMetadata());
        }
        if (request.getLayout() != null) {
            page.setLayout(request.getLayout());
        }
        if (request.getTags() != null) {
            page.setTags(request.getTags());
        }
        return page;
    }
}