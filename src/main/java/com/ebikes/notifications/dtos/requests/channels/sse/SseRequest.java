package com.ebikes.notifications.dtos.requests.channels.sse;

import java.io.Serializable;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record SseRequest(
    @NotBlank String userId,
    @NotBlank String type,
    @NotBlank String entityId,
    @NotBlank String reference,
    @NotBlank String title,
    @NotBlank String message,
    @NotNull Priority priority,
    @NotNull Map<String, String> metadata,
    String actionUrl)
    implements Serializable {

  public enum Priority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
  }

  public SseRequest {
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    priority = priority != null ? priority : Priority.NORMAL;
  }

  @JsonIgnore
  public String userId() {
    return userId;
  }
}
