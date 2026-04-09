package com.ebikes.notifications.dtos.events.incoming;

import java.io.Serializable;

public record OrganizationCreatedEvent(
    String displayName, String organizationId, String ownerId, String serviceReference)
    implements Serializable {}
