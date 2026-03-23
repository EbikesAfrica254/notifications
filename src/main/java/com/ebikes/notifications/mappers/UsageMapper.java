package com.ebikes.notifications.mappers;

import java.time.OffsetDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ebikes.notifications.dtos.responses.usage.UsageResponse;
import com.ebikes.notifications.enums.ChannelType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UsageMapper {

  default UsageResponse toResponse(
      String scope, OffsetDateTime from, OffsetDateTime to, ChannelType channel, Long count) {
    return new UsageResponse(scope, from, to, channel, count);
  }
}
