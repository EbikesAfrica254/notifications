package com.ebikes.notifications.dtos.responses.deliveries;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.notifications.enums.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

public record DeliveryResponse(
    Integer attemptNumber,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime attemptedAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime completedAt,
    BigDecimal costAmount,
    String costCurrency,
    String errorCode,
    String errorMessage,
    UUID id,
    UUID notificationId,
    String providerMessageId,
    DeliveryStatus status) {}
