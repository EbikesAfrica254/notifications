package com.ebikes.notifications.support.fixtures;

import java.util.UUID;

import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationStatus;

public final class NotificationFixtures {

  private NotificationFixtures() {}

  public static Notification forOrganization(String organizationId, String recipient) {
    return base(organizationId, null, recipient, ChannelType.EMAIL).build();
  }

  public static Notification forOrganizationAndBranch(
      String organizationId, String branchId, String recipient) {
    return base(organizationId, branchId, recipient, ChannelType.EMAIL).build();
  }

  public static Notification forOrganizationAndBranch(
      String organizationId, String branchId, String recipient, ChannelType channel) {
    return base(organizationId, branchId, recipient, channel).build();
  }

  private static Notification.NotificationBuilder<?, ?> base(
      String organizationId, String branchId, String recipient, ChannelType channel) {
    Notification.NotificationBuilder<?, ?> builder =
        Notification.builder()
            .channel(channel)
            .messageBody("Test message body")
            .organizationId(organizationId)
            .branchId(branchId)
            .recipient(recipient)
            .serviceReference("ref-" + UUID.randomUUID())
            .status(NotificationStatus.PENDING);

    if (channel == ChannelType.EMAIL) {
      builder.messageSubject("Test message subject");
    }

    return builder;
  }
}
