package com.ebikes.notifications.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ebikes.notifications.database.entities.OrganizationChannelPreference;
import com.ebikes.notifications.dtos.responses.preferences.organization.OrganizationPreferenceResponse;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface OrganizationPreferenceMapper {

  OrganizationPreferenceResponse toResponse(OrganizationChannelPreference preference);
}
