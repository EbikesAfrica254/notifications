package com.ebikes.notifications.support.fixtures;

import com.ebikes.notifications.dtos.events.incoming.OrganizationCreatedEvent;

public final class EventFixtures {

  private EventFixtures() {}

  public static OrganizationCreatedEvent organizationCreatedEvent() {
    return new OrganizationCreatedEvent(
        "Test Organization",
        SecurityFixtures.TEST_ORGANIZATION_ID,
        SecurityFixtures.TEST_USER_ID,
        "test-services-reference");
  }
}
