package com.ebikes.notifications.services.deliveries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.repositories.DeliveryRepository;
import com.ebikes.notifications.database.repositories.NotificationRepository;
import com.ebikes.notifications.dtos.responses.deliveries.DeliveryResponse;
import com.ebikes.notifications.enums.DeliveryStatus;
import com.ebikes.notifications.enums.UserRole;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.DeliveryMapper;
import com.ebikes.notifications.support.audit.AuditTemplate;
import com.ebikes.notifications.support.audit.ThrowingRunnable;
import com.ebikes.notifications.support.audit.ThrowingSupplier;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.fixtures.DeliveryFixtures;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;
import com.ebikes.notifications.support.fixtures.SecurityFixtures;

@DisplayName("DeliveryService")
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

  private static final String TEST_ORGANIZATION_ID = SecurityFixtures.TEST_ORGANIZATION_ID;
  private static final String TEST_EMAIL = SecurityFixtures.TEST_EMAIL;
  private static final UUID DELIVERY_ID = UUID.randomUUID();
  private static final UUID NOTIFICATION_ID = UUID.randomUUID();

  @Mock private AuditTemplate auditTemplate;
  @Mock private DeliveryMapper deliveryMapper;
  @Mock private DeliveryRepository deliveryRepository;
  @Mock private NotificationRepository notificationRepository;

  private DeliveryService service;

  @BeforeEach
  void setUp() {
    service =
        new DeliveryService(
            auditTemplate, deliveryMapper, deliveryRepository, notificationRepository);
  }

  @Nested
  @DisplayName("createAttempt")
  class CreateAttempt {

    @Test
    @DisplayName("should create and persist delivery attempt for existing notification")
    void shouldCreateDeliveryAttempt() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
      when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
      when(auditTemplate.execute(any(), any(), any(), (ThrowingSupplier) any()))
          .thenAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get());
      Delivery result = service.createAttempt(NOTIFICATION_ID, 1);

      assertThat(result.getStatus()).isEqualTo(DeliveryStatus.PENDING);
      assertThat(result.getAttemptNumber()).isEqualTo(1);
      verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when notification not found")
    void shouldThrowWhenNotificationNotFound() {
      when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.createAttempt(NOTIFICATION_ID, 1))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should return delivery when found")
    void shouldReturnDeliveryWhenFound() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));

      assertThat(service.findById(DELIVERY_ID)).isEqualTo(delivery);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findById(DELIVERY_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findByNotificationId")
  class FindByNotificationId {

    @AfterEach
    void tearDown() {
      ExecutionContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("should return mapped delivery responses sorted by attempt number")
    void shouldReturnMappedDeliveryResponses() {
      SecurityFixtures.setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      DeliveryResponse response =
          new DeliveryResponse(
              1,
              null,
              null,
              null,
              null,
              null,
              null,
              DELIVERY_ID,
              NOTIFICATION_ID,
              null,
              DeliveryStatus.PENDING);
      when(deliveryRepository.findAll(any(Specification.class), any(Sort.class)))
          .thenReturn(List.of(delivery));
      when(deliveryMapper.toResponse(delivery)).thenReturn(response);

      List<DeliveryResponse> result = service.findByNotificationId(NOTIFICATION_ID);

      assertThat(result).containsExactly(response);
    }
  }

  @Nested
  @DisplayName("recordDelivered")
  class RecordDelivered {

    @Test
    @DisplayName("should mark delivery as delivered and save")
    void shouldMarkDeliveredAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), (ThrowingRunnable<?>) any());

      service.recordDelivered(new BigDecimal("0.50"), "KES", DELIVERY_ID, "msg-123", Map.of());

      assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
      verify(deliveryRepository).save(delivery);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when delivery not found")
    void shouldThrowWhenDeliveryNotFound() {
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.recordDelivered(BigDecimal.ZERO, "KES", DELIVERY_ID, "msg-123", Map.of()))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("recordChannelDisabled")
  class RecordChannelDisabled {

    @Test
    @DisplayName("should mark delivery as channel disabled and save")
    void shouldMarkChannelDisabledAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), (ThrowingRunnable<?>) any());

      service.recordChannelDisabled(DELIVERY_ID);

      assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CHANNEL_DISABLED);
      verify(deliveryRepository).save(delivery);
    }
  }

  @Nested
  @DisplayName("recordFailed")
  class RecordFailed {

    @Test
    @DisplayName("should mark delivery as failed and save")
    void shouldMarkFailedAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), (ThrowingRunnable<?>) any());

      service.recordFailed(DELIVERY_ID, "ERR_001", "Delivery failed", Map.of());

      assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
      verify(deliveryRepository).save(delivery);
    }
  }

  @Nested
  @DisplayName("recordInvalidRecipient")
  class RecordInvalidRecipient {

    @Test
    @DisplayName("should mark delivery as invalid recipient and save")
    void shouldMarkInvalidRecipientAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), (ThrowingRunnable<?>) any());

      service.recordInvalidRecipient(DELIVERY_ID, "ERR_INVALID", "Invalid recipient");

      assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.INVALID_RECIPIENT);
      verify(deliveryRepository).save(delivery);
    }
  }

  @Nested
  @DisplayName("recordOffline")
  class RecordOffline {

    @Test
    @DisplayName("should mark delivery as offline and save")
    void shouldMarkOfflineAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), (ThrowingRunnable<?>) any());

      service.recordOffline(DELIVERY_ID);

      assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.OFFLINE);
      verify(deliveryRepository).save(delivery);
    }
  }

  @Nested
  @DisplayName("recordRateLimited")
  class RecordRateLimited {

    @Test
    @DisplayName("should mark delivery as rate limited and save")
    void shouldMarkRateLimitedAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));
      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));

      service.recordRateLimited(DELIVERY_ID);

      assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.RATE_LIMITED);
      verify(deliveryRepository).save(delivery);
    }
  }

  @Nested
  @DisplayName("recordTimeout")
  class RecordTimeout {

    @Test
    @DisplayName("should mark delivery as timed out and save")
    void shouldMarkTimeoutAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), (ThrowingRunnable<?>) any());

      service.recordTimeout(DELIVERY_ID);

      assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.TIMEOUT);
      verify(deliveryRepository).save(delivery);
    }
  }
}
