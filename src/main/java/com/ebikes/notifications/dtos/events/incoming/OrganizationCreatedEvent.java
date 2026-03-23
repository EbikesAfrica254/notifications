package com.ebikes.notifications.dtos.events.incoming;

import java.io.Serializable;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record OrganizationCreatedEvent(
    @NotBlank String organizationId,
    @NotBlank String eventType,
    @NotBlank String serviceReference,
    Instant timestamp)
    implements Serializable {

  public OrganizationCreatedEvent {
    timestamp = timestamp != null ? timestamp : Instant.now();
  }
}
