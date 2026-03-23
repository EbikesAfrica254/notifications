package com.ebikes.notifications.dtos.responses.notifications;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
    String branchId,
    ChannelType channel,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime createdAt,
    String createdBy,
    UUID id,
    String messageBody,
    String messageSubject,
    String organizationId,
    String recipient,
    String serviceReference,
    NotificationStatus status,
    UUID templateId,
    Integer templateVersion,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime updatedAt,
    String updatedBy,
    Map<String, Object> variables)
    implements Serializable {}
