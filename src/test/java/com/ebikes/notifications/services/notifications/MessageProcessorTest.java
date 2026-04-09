package com.ebikes.notifications.services.notifications;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.exceptions.RateLimitException;
import com.ebikes.notifications.exceptions.RecipientOfflineException;
import com.ebikes.notifications.services.channels.ChannelService;
import com.ebikes.notifications.services.deliveries.DeliveryService;
import com.ebikes.notifications.services.preferences.UserPreferenceService;
import com.ebikes.notifications.support.fixtures.DeliveryFixtures;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;
import com.ebikes.notifications.support.fixtures.SecurityFixtures;

@DisplayName("MessageProcessor")
@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

  private static final String TEST_ORGANIZATION_ID = SecurityFixtures.TEST_ORGANIZATION_ID;
  private static final String TEST_EMAIL = SecurityFixtures.TEST_EMAIL;

  @Mock private ChannelService channelService;
  @Mock private DeliveryService deliveryService;
  @Mock private NotificationService notificationService;
  @Mock private UserPreferenceService userPreferenceService;

  private MessageProcessor buildProcessor(List<ChannelService> services) {
    return new MessageProcessor(
        services, deliveryService, notificationService, userPreferenceService);
  }

  private NotificationRequest emailRequest() {
    return new NotificationRequest(
        null,
        NotificationCategory.TRANSACTIONAL,
        ChannelType.EMAIL,
        "orders.completed",
        TEST_ORGANIZATION_ID,
        TEST_EMAIL,
        "ref-" + UUID.randomUUID(),
        null,
        "ORDER_COMPLETED",
        null,
        Map.of());
  }

  private ChannelResponse channelResponse() {
    return new ChannelResponse(
        new BigDecimal("0.50"), "KES", Map.of(), "msg-123", "ses", OffsetDateTime.now());
  }

  @Nested
  @DisplayName("when suppressed by user preference")
  class WhenSuppressed {

    @Test
    @DisplayName("should return early without creating a notification")
    void shouldReturnEarlyWhenSuppressed() {
      MessageProcessor processor = buildProcessor(List.of());
      NotificationRequest request = emailRequest();
      when(userPreferenceService.isEnabled(request)).thenReturn(false);

      processor.process(request);

      verify(notificationService, never()).create(any());
      verify(deliveryService, never()).createAttempt(any(), anyInt());
    }
  }

  @Nested
  @DisplayName("when no channel service is registered")
  class WhenChannelNotFound {

    @Test
    @DisplayName("should record channel disabled and mark notification failed")
    void shouldRecordChannelDisabledAndMarkFailed() {
      MessageProcessor processor = buildProcessor(List.of());
      NotificationRequest request = emailRequest();

      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);

      when(userPreferenceService.isEnabled(request)).thenReturn(true);
      when(notificationService.create(request)).thenReturn(notification);
      when(deliveryService.createAttempt(notification.getId(), 1)).thenReturn(delivery);

      processor.process(request);

      verify(deliveryService).recordChannelDisabled(delivery.getId());
      verify(notificationService).markFailed(notification.getId());
    }
  }

  @Nested
  @DisplayName("when delivery succeeds")
  class WhenDeliverySucceeds {

    @Test
    @DisplayName("should create, process, send, and record delivery as delivered")
    void shouldDeliverSuccessfully() {
      when(channelService.getChannelType()).thenReturn(ChannelType.EMAIL);
      MessageProcessor processor = buildProcessor(List.of(channelService));
      NotificationRequest request = emailRequest();

      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      ChannelResponse response = channelResponse();

      when(userPreferenceService.isEnabled(request)).thenReturn(true);
      when(notificationService.create(request)).thenReturn(notification);
      when(deliveryService.createAttempt(notification.getId(), 1)).thenReturn(delivery);
      when(channelService.send(notification)).thenReturn(response);

      processor.process(request);

      verify(notificationService).create(request);
      verify(notificationService).markProcessing(notification.getId());
      verify(deliveryService).createAttempt(notification.getId(), 1);
      verify(channelService).send(notification);
      verify(deliveryService)
          .recordDelivered(
              eq(response.costAmount()),
              eq(response.costCurrency()),
              eq(delivery.getId()),
              eq(response.providerMessageId()),
              any());
      verify(notificationService).markDelivered(notification.getId());
    }
  }

  @Nested
  @DisplayName("when recipient is offline")
  class WhenRecipientOffline {

    @Test
    @DisplayName("should record offline, mark failed, schedule retry, and not rethrow")
    void shouldHandleOfflineGracefully() {
      when(channelService.getChannelType()).thenReturn(ChannelType.EMAIL);
      MessageProcessor processor = buildProcessor(List.of(channelService));
      NotificationRequest request = emailRequest();

      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);

      when(userPreferenceService.isEnabled(request)).thenReturn(true);
      when(notificationService.create(request)).thenReturn(notification);
      when(deliveryService.createAttempt(notification.getId(), 1)).thenReturn(delivery);
      when(channelService.send(notification))
          .thenThrow(new RecipientOfflineException(notification.getId(), TEST_EMAIL));

      processor.process(request);

      verify(deliveryService).recordOffline(any());
      verify(notificationService).markFailed(notification.getId());
      verify(notificationService).scheduleRetry(notification.getId());
    }
  }

  @Nested
  @DisplayName("when an invalid state exception occurs")
  class WhenInvalidState {

    @Test
    @DisplayName("should record failed, mark notification failed, and not rethrow")
    void shouldHandleInvalidStateGracefully() {
      when(channelService.getChannelType()).thenReturn(ChannelType.EMAIL);
      MessageProcessor processor = buildProcessor(List.of(channelService));
      NotificationRequest request = emailRequest();

      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);

      when(userPreferenceService.isEnabled(request)).thenReturn(true);
      when(notificationService.create(request)).thenReturn(notification);
      when(deliveryService.createAttempt(notification.getId(), 1)).thenReturn(delivery);
      when(channelService.send(notification))
          .thenThrow(new InvalidStateException(ResponseCode.INVALID_STATE, "bad state"));

      processor.process(request);

      verify(deliveryService).recordFailed(eq(delivery.getId()), any(), any(), any());
      verify(notificationService).markFailed(notification.getId());
      verify(notificationService, never()).scheduleRetry(any());
    }
  }

  @Nested
  @DisplayName("when rate limited")
  class WhenRateLimited {

    @Test
    @DisplayName("should record rate limited, schedule retry, and rethrow")
    void shouldRecordRateLimitedAndRethrow() {
      when(channelService.getChannelType()).thenReturn(ChannelType.EMAIL);
      MessageProcessor processor = buildProcessor(List.of(channelService));
      NotificationRequest request = emailRequest();

      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      RateLimitException ex =
          new RateLimitException(ResponseCode.RATE_LIMIT_EXCEEDED, "rate limited");

      when(userPreferenceService.isEnabled(request)).thenReturn(true);
      when(notificationService.create(request)).thenReturn(notification);
      when(deliveryService.createAttempt(notification.getId(), 1)).thenReturn(delivery);
      when(channelService.send(notification)).thenThrow(ex);

      assertThatThrownBy(() -> processor.process(request)).isSameAs(ex);

      verify(deliveryService).recordRateLimited(any());
      verify(notificationService).scheduleRetry(notification.getId());
      verify(notificationService, never()).markFailed(any());
    }
  }

  @Nested
  @DisplayName("when external service reports invalid recipient")
  class WhenInvalidRecipient {

    @Test
    @DisplayName("should record invalid recipient, mark failed, and rethrow")
    void shouldRecordInvalidRecipientAndRethrow() {
      when(channelService.getChannelType()).thenReturn(ChannelType.EMAIL);
      MessageProcessor processor = buildProcessor(List.of(channelService));
      NotificationRequest request = emailRequest();

      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      ExternalServiceException ex =
          new ExternalServiceException("ses", "bad address", ResponseCode.INVALID_RECIPIENT);

      when(userPreferenceService.isEnabled(request)).thenReturn(true);
      when(notificationService.create(request)).thenReturn(notification);
      when(deliveryService.createAttempt(notification.getId(), 1)).thenReturn(delivery);
      when(channelService.send(notification)).thenThrow(ex);

      assertThatThrownBy(() -> processor.process(request)).isSameAs(ex);

      verify(deliveryService).recordInvalidRecipient(eq(delivery.getId()), any(), any());
      verify(notificationService).markFailed(notification.getId());
      verify(deliveryService, never()).recordFailed(any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("when external service fails with non-recipient error")
  class WhenExternalServiceFails {

    @Test
    @DisplayName("should record failed, mark failed, and rethrow")
    void shouldRecordFailedAndRethrow() {
      when(channelService.getChannelType()).thenReturn(ChannelType.EMAIL);
      MessageProcessor processor = buildProcessor(List.of(channelService));
      NotificationRequest request = emailRequest();

      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      ExternalServiceException ex =
          new ExternalServiceException(
              "ses", "connection error", ResponseCode.EXTERNAL_SERVICE_ERROR);

      when(userPreferenceService.isEnabled(request)).thenReturn(true);
      when(notificationService.create(request)).thenReturn(notification);
      when(deliveryService.createAttempt(notification.getId(), 1)).thenReturn(delivery);
      when(channelService.send(notification)).thenThrow(ex);

      assertThatThrownBy(() -> processor.process(request)).isSameAs(ex);

      verify(deliveryService).recordFailed(eq(delivery.getId()), any(), any(), any());
      verify(notificationService).markFailed(notification.getId());
      verify(deliveryService, never()).recordInvalidRecipient(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("when an unexpected exception occurs")
  class WhenUnexpectedException {

    @Test
    @DisplayName("should record failed with PROCESSING_ERROR, mark failed, and rethrow")
    void shouldRecordProcessingErrorAndRethrow() {
      when(channelService.getChannelType()).thenReturn(ChannelType.EMAIL);
      MessageProcessor processor = buildProcessor(List.of(channelService));
      NotificationRequest request = emailRequest();

      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      Delivery delivery = DeliveryFixtures.pending(notification, TEST_ORGANIZATION_ID);
      RuntimeException ex = new RuntimeException("unexpected");

      when(userPreferenceService.isEnabled(request)).thenReturn(true);
      when(notificationService.create(request)).thenReturn(notification);
      when(deliveryService.createAttempt(notification.getId(), 1)).thenReturn(delivery);
      when(channelService.send(notification)).thenThrow(ex);

      assertThatThrownBy(() -> processor.process(request)).isSameAs(ex);

      verify(deliveryService)
          .recordFailed(eq(delivery.getId()), eq("PROCESSING_ERROR"), any(), any());
      verify(notificationService).markFailed(notification.getId());
    }
  }
}
