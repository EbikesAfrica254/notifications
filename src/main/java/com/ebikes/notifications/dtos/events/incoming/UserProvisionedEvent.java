package com.ebikes.notifications.dtos.events.incoming;

import java.io.Serializable;
import java.time.Instant;

public record UserProvisionedEvent(
    String keycloakUserId,
    String organizationId,
    String branchId,
    String email,
    String eventType,
    String serviceReference,
    Instant timestamp)
    implements Serializable {}
