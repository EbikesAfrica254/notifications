package com.ebikes.notifications.database.specifications;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.notifications.database.entities.Delivery;

public final class DeliverySpecifications {

  public static final String FIELD_ATTEMPT_NUMBER = "attemptNumber";
  public static final String FIELD_NOTIFICATION_ID = "id";

  private DeliverySpecifications() {
    // prevent instantiation
  }

  public static Specification<Delivery> forNotification(UUID notificationId) {
    return AuthorizationSpecifications.withNotificationFetch()
        .and(hasNotificationId(notificationId));
  }

  public static Specification<Delivery> hasNotificationId(UUID notificationId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(
            AuthorizationSpecifications.notificationJoin(root).get(FIELD_NOTIFICATION_ID),
            notificationId);
  }
}
