package com.ebikes.notifications.support.fixtures;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import com.ebikes.notifications.constants.EventConstants.Source;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;

import net.datafaker.Faker;

public final class NotificationRequestFixtures {

  private static final Faker FAKER = new Faker();

  private NotificationRequestFixtures() {}

  public static NotificationRequest accountVerification() {
    return base(
        ChannelType.EMAIL,
        "iam.user-extension.activation-requested",
        "ACCOUNT_VERIFICATION",
        Map.of("name", FAKER.name().firstName()));
  }

  private static NotificationRequest base(
      ChannelType channel,
      String eventType,
      String templateName,
      Map<String, Serializable> variables) {
    return new NotificationRequest(
        null,
        NotificationCategory.SECURITY,
        channel,
        eventType,
        UUID.randomUUID().toString(),
        FAKER.internet().emailAddress(),
        Source.serviceReference(),
        UUID.randomUUID().toString(),
        templateName,
        null,
        variables);
  }
}
