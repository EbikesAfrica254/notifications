package com.ebikes.notifications.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ebikes.notifications.database.entities.UserChannelPreference;
import com.ebikes.notifications.dtos.responses.preferences.user.UserPreferenceResponse;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserPreferenceMapper {

  UserPreferenceResponse toResponse(UserChannelPreference preference);
}
