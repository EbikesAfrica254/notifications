package com.ebikes.notifications.support.fixtures;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.enums.DeliveryStatus;

public final class DeliveryFixtures {

  private DeliveryFixtures() {}

  public static Delivery pending(Notification notification, String organizationId) {
    return base(notification, organizationId, 1).build();
  }

  public static Delivery pending(
      Notification notification, String organizationId, int attemptNumber) {
    return base(notification, organizationId, attemptNumber).build();
  }

  private static Delivery.DeliveryBuilder<?, ?> base(
      Notification notification, String organizationId, int attemptNumber) {
    return Delivery.builder()
        .attemptNumber(attemptNumber)
        .notification(notification)
        .organizationId(organizationId)
        .status(DeliveryStatus.PENDING);
  }
}
