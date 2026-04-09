package com.ebikes.notifications.dtos.responses.templates;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.ebikes.notifications.database.models.TemplateVariable;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.TemplateContentType;
import com.fasterxml.jackson.annotation.JsonFormat;

public record TemplateResponse(
    String bodyTemplate,
    ChannelType channel,
    TemplateContentType templateContentType,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime createdAt,
    String createdBy,
    UUID id,
    boolean isActive,
    String name,
    String subject,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime updatedAt,
    String updatedBy,
    List<TemplateVariable> variableDefinitions,
    int version) {}
