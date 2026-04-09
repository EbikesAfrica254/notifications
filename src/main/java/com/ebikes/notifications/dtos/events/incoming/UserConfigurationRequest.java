package com.ebikes.notifications.dtos.events.incoming;

import java.io.Serializable;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record UserConfigurationRequest(
    @NotBlank String eventType,
    @NotBlank String keycloakUserId,
    @NotBlank String organizationId,
    @NotBlank String serviceReference,
    Instant timestamp)
    implements Serializable {

  public UserConfigurationRequest {
    timestamp = timestamp != null ? timestamp : Instant.now();
  }
}
