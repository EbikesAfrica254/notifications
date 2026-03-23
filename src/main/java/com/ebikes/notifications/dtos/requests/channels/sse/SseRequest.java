package com.ebikes.notifications.dtos.requests.channels.sse;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

public record SseRequest(
    @NotBlank(message = "Event data is required") String data,
    @NotBlank(message = "User ID is required") String userId)
    implements Serializable {}
