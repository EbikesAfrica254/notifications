package com.ebikes.notifications.mappers;

import org.mapstruct.Mapper;

import com.ebikes.notifications.database.entities.Outbox;
import com.ebikes.notifications.dtos.responses.outbox.OutboxResponse;

@Mapper(componentModel = "spring")
public interface OutboxMapper {

  OutboxResponse toResponse(Outbox outbox);
}
