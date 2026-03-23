package com.ebikes.notifications.dtos.responses.preferences.user;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.fasterxml.jackson.annotation.JsonFormat;

public record UserPreferenceResponse(
    NotificationCategory category,
    ChannelType channel,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime createdAt,
    Boolean enabled,
    UUID id,
    String organizationId,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime updatedAt,
    String userId,
    Integer version) {}
