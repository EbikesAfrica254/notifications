package com.ebikes.notifications.dtos.responses.templates;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.TemplateContentType;
import com.fasterxml.jackson.annotation.JsonFormat;

public record TemplateSummaryResponse(
    ChannelType channel,
    TemplateContentType templateContentType,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime createdAt,
    String createdBy,
    UUID id,
    boolean isActive,
    String name,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime updatedAt,
    String updatedBy,
    int version) {}
