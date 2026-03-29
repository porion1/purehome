package com.purehome.uicore.dto.mapper;

import com.purehome.uicore.dto.response.VersionResponse;
import com.purehome.uicore.model.PageVersion;
import com.purehome.uicore.model.VersionNode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * FAANG-GRADE VERSION MAPPER
 *
 * Maps between PageVersion, VersionNode entities and VersionResponse DTO
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Mapper(componentModel = "spring", uses = {PageMapper.class})
public interface VersionMapper {

    VersionMapper INSTANCE = Mappers.getMapper(VersionMapper.class);

    /**
     * Convert PageVersion to VersionResponse
     */
    @Mapping(source = "id", target = "versionId")
    @Mapping(source = "changeType", target = "changeType", qualifiedByName = "pageVersionChangeTypeToString")
    @Mapping(source = "pageSnapshot", target = "pageSnapshot")
    VersionResponse toResponse(PageVersion version);

    /**
     * Convert VersionNode to VersionResponse
     */
    @Mapping(source = "id", target = "versionId")
    @Mapping(source = "changeType", target = "changeType", qualifiedByName = "versionNodeChangeTypeToString")
    @Mapping(target = "pageSnapshot", ignore = true)
    VersionResponse toResponse(VersionNode node);

    /**
     * Convert PageVersion.ChangeType enum to String
     */
    @Named("pageVersionChangeTypeToString")
    default String pageVersionChangeTypeToString(PageVersion.ChangeType changeType) {
        return changeType != null ? changeType.getCode() : null;
    }

    /**
     * Convert VersionNode.ChangeType enum to String
     */
    @Named("versionNodeChangeTypeToString")
    default String versionNodeChangeTypeToString(VersionNode.ChangeType changeType) {
        return changeType != null ? changeType.getCode() : null;
    }
}