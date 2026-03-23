package com.ebikes.notifications.dtos.responses.channels;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ChannelResponse(
    BigDecimal costAmount,
    String costCurrency,
    Map<String, Serializable> metadata,
    String providerMessageId,
    String providerName,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime sentAt)
    implements Serializable {}
