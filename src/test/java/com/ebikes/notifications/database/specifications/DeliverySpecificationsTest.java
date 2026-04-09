package com.ebikes.notifications.database.specifications;

import static com.ebikes.notifications.support.fixtures.SecurityFixtures.OTHER_ORGANIZATION_ID;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_EMAIL;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_ORGANIZATION_ID;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_PHONE_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.repositories.DeliveryRepository;
import com.ebikes.notifications.database.repositories.NotificationRepository;
import com.ebikes.notifications.support.fixtures.DeliveryFixtures;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractRepositoryTest;

@DisplayName("DeliverySpecifications")
class DeliverySpecificationsTest extends AbstractRepositoryTest {

  @Autowired private DeliveryRepository deliveryRepository;
  @Autowired private NotificationRepository notificationRepository;

  private Notification ownNotification;
  private Delivery firstDelivery;
  private Delivery secondDelivery;
  private Delivery otherDelivery;

  @BeforeEach
  void setUp() {
    ownNotification =
        notificationRepository.save(
            NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL));

    Notification otherNotification =
        notificationRepository.save(
            NotificationFixtures.forOrganization(OTHER_ORGANIZATION_ID, TEST_PHONE_NUMBER));

    firstDelivery =
        deliveryRepository.save(DeliveryFixtures.pending(ownNotification, TEST_ORGANIZATION_ID, 1));

    secondDelivery =
        deliveryRepository.save(DeliveryFixtures.pending(ownNotification, TEST_ORGANIZATION_ID, 2));

    otherDelivery =
        deliveryRepository.save(
            DeliveryFixtures.pending(otherNotification, OTHER_ORGANIZATION_ID, 1));
  }

  @AfterEach
  void tearDown() {
    deliveryRepository.deleteAll();
    notificationRepository.deleteAll();
  }

  @Test
  @DisplayName("hasNotificationId returns only deliveries for the given notification")
  void hasNotificationIdReturnsMatchingDeliveries() {
    List<Delivery> results =
        deliveryRepository.findAll(
            DeliverySpecifications.hasNotificationId(ownNotification.getId()));

    assertThat(results)
        .hasSize(2)
        .extracting(Delivery::getId)
        .contains(firstDelivery.getId(), secondDelivery.getId());
  }

  @Test
  @DisplayName("hasNotificationId does not return deliveries for another notification")
  void hasNotificationIdExcludesOtherNotification() {
    List<Delivery> results =
        deliveryRepository.findAll(
            DeliverySpecifications.hasNotificationId(ownNotification.getId()));

    assertThat(results).isNotEmpty();
    assertThat(results).extracting(Delivery::getId).doesNotContain(otherDelivery.getId());
  }

  @Test
  @DisplayName("forNotification returns deliveries with notification eagerly fetched")
  void forNotificationEagerlyFetchesNotification() {
    List<Delivery> results =
        deliveryRepository.findAll(DeliverySpecifications.forNotification(ownNotification.getId()));

    assertThat(results)
        .hasSize(2)
        .allSatisfy(
            delivery ->
                assertThat(delivery.getNotification().getId()).isEqualTo(ownNotification.getId()));
  }

  @Test
  @DisplayName("forNotification does not return deliveries for another notification")
  void forNotificationExcludesOtherNotification() {
    List<Delivery> results =
        deliveryRepository.findAll(DeliverySpecifications.forNotification(ownNotification.getId()));

    assertThat(results).isNotEmpty();
    assertThat(results).extracting(Delivery::getId).doesNotContain(otherDelivery.getId());
  }
}
