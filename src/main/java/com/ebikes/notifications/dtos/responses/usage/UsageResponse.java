package com.ebikes.notifications.dtos.responses.usage;

import java.time.OffsetDateTime;

import com.ebikes.notifications.enums.ChannelType;
import com.fasterxml.jackson.annotation.JsonFormat;

public record UsageResponse(
    String scope,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime from,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime to,
    ChannelType channel,
    Long count) {}
