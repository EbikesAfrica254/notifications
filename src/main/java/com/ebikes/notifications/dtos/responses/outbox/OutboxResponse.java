package com.ebikes.notifications.dtos.responses.outbox;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.notifications.enums.OutboxStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OutboxResponse(
    UUID id,
    OutboxStatus status,
    String eventType,
    Integer retryCount,
    String routingKey,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt)
    implements Serializable {}
