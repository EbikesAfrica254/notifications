package com.ebikes.notifications.services.notifications;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.dtos.requests.channels.email.EmailRequest;
import com.ebikes.notifications.dtos.requests.channels.sms.SmsRequest;
import com.ebikes.notifications.dtos.requests.channels.sse.SseRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.exceptions.RateLimitException;
import com.ebikes.notifications.publishers.AuditEventPublisher;
import com.ebikes.notifications.services.channels.email.EmailChannelService;
import com.ebikes.notifications.services.channels.sms.SmsChannelService;
import com.ebikes.notifications.services.channels.sse.SseChannelService;
import com.ebikes.notifications.services.channels.whatsapp.WhatsAppChannelService;
import com.ebikes.notifications.services.deliveries.DeliveryService;
import com.ebikes.notifications.services.preferences.UserPreferenceService;
import com.ebikes.notifications.support.audit.AuditMetadataBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ebikes.notifications.constants.ApplicationConstants.SYSTEM_ID;
import static com.ebikes.notifications.constants.EventConstants.EventTypes;
import static com.ebikes.notifications.constants.EventConstants.RoutingKeys;

@RequiredArgsConstructor
@Service
@Slf4j
public class MessageProcessor {

  private static final String NOTIFICATIONS = "NOTIFICATIONS";
  private static final String ROUTING_KEY = RoutingKeys.audit("notifications");

  private final AuditEventPublisher auditEventPublisher;
  private final DeliveryService deliveryService;
  private final Optional<EmailChannelService> emailChannelService;
  private final NotificationService notificationService;
  private final Optional<SmsChannelService> smsChannelService;
  private final Optional<SseChannelService> sseChannelService;
  private final UserPreferenceService userPreferenceService;
  private final Optional<WhatsAppChannelService> whatsAppChannelService;

  public void process(NotificationRequest request) {
    if (!userPreferenceService.isEnabled(
        request.organizationId(), request.subjectUserId(), request.channel(), request.category())) {
      log.info(
          "Notification suppressed by preference - serviceReference={} channel={} category={}"
              + " recipient={}",
          request.serviceReference(),
          request.channel(),
          request.category(),
          request.recipient());
      return;
    }

    log.info(
        "Processing notification - serviceReference={} channel={} category={} recipient={}",
        request.serviceReference(),
        request.channel(),
        request.category(),
        request.recipient());

    Notification notification = notificationService.create(request);

    log.info(
        "Notification created - serviceReference={} notification.getId()={}",
        request.serviceReference(),
        notification.getId());

    notificationService.markProcessing(notification.getId());

    Delivery delivery = deliveryService.createAttempt(notification.getId(), 1);

    try {
      ChannelResponse channelResponse = routeToChannel(notification, request.variables());

      deliveryService.recordDelivered(
          channelResponse.costAmount(),
          channelResponse.costCurrency(),
          delivery.getId(),
          channelResponse.providerMessageId(),
          channelResponse.metadata());

      notificationService.markDelivered(notification.getId());

      log.info(
          "Notification delivered - serviceReference={} notification.getId()={} provider={}",
          request.serviceReference(),
          notification.getId(),
          channelResponse.providerName());

      auditEventPublisher.publishSuccess(
          notification.getId(),
          NOTIFICATIONS,
          EventTypes.Notifications.DELIVERED,
          AuditMetadataBuilder.forNotification(
              notification, Map.of("providerName", channelResponse.providerName())),
          notification.getOrganizationId(),
          ROUTING_KEY,
          SYSTEM_ID);

    } catch (InvalidStateException e) {
      log.error(
          "Channel disabled - serviceReference={} notification.getId()={} channel={} error={}",
          request.serviceReference(),
          notification.getId(),
          notification.getChannel(),
          e.getMessage());

      deliveryService.recordChannelDisabled(delivery.getId());
      notificationService.markFailed(notification.getId());

      auditEventPublisher.publishFailure(
          notification.getId(),
          NOTIFICATIONS,
          EventTypes.Notifications.FAILED,
          e.getMessage(),
          AuditMetadataBuilder.forNotification(
              notification, Map.of("configuration", "CHANNEL_DISABLED")),
          notification.getOrganizationId(),
          ROUTING_KEY);

    } catch (RateLimitException e) {
      log.warn(
          "Delivery rate limited - serviceReference={} notification.getId()={} error={}",
          request.serviceReference(),
          notification.getId(),
          e.getMessage());

      deliveryService.recordRateLimited(delivery.getId(), delivery.getAttemptNumber());
      notificationService.scheduleRetry(notification.getId());
      throw e;

    } catch (ExternalServiceException e) {
      log.error(
          "Channel delivery failed - serviceReference={} notification.getId()={} error={}",
          request.serviceReference(),
          notification.getId(),
          e.getMessage(),
          e);

      if (e.getResponseCode() == ResponseCode.INVALID_RECIPIENT) {
        deliveryService.recordInvalidRecipient(
            delivery.getId(), e.getResponseCode().getCode(), e.getMessage());
      } else {
        deliveryService.recordFailed(
            delivery.getId(), e.getResponseCode().getCode(), e.getMessage(), null, null);
      }

      notificationService.markFailed(notification.getId());

      auditEventPublisher.publishFailure(
          notification.getId(),
          "NOTIFICATION",
          EventTypes.Notifications.FAILED,
          e.getMessage(),
          AuditMetadataBuilder.forNotification(
              notification, Map.of("error", e.getResponseCode().getCode())),
          notification.getOrganizationId(),
          ROUTING_KEY);
      throw e;

    } catch (Exception e) {
      log.error(
          "Unexpected error processing notification - serviceReference={} notification.getId()={}",
          request.serviceReference(),
          notification.getId(),
          e);

      deliveryService.recordFailed(
          delivery.getId(), "PROCESSING_ERROR", e.getMessage(), null, null);
      notificationService.markFailed(notification.getId());

      auditEventPublisher.publishFailure(
          notification.getId(),
          NOTIFICATIONS,
          EventTypes.Notifications.FAILED,
          e.getMessage(),
          AuditMetadataBuilder.forNotification(notification, Map.of("error", "PROCESSING_ERROR")),
          notification.getOrganizationId(),
          ROUTING_KEY);
      throw e;
    }
  }

  private ChannelResponse routeToChannel(
      Notification notification, Map<String, Serializable> variables) {
    return switch (notification.getChannel()) {
      case EMAIL ->
          emailChannelService
              .orElseThrow(
                  () ->
                      new InvalidStateException(
                          ResponseCode.INVALID_STATE, "Email channel is disabled in configuration"))
              .send(
                  new EmailRequest(
                      notification.getMessageBody(),
                      notification.getRecipient(),
                      notification.getMessageSubject()));
      case SMS ->
          smsChannelService
              .orElseThrow(
                  () ->
                      new InvalidStateException(
                          ResponseCode.INVALID_STATE, "SMS channel is disabled in configuration"))
              .send(
                  new SmsRequest(
                      notification.getMessageBody(), List.of(notification.getRecipient())));
      case SSE ->
          sseChannelService
              .orElseThrow(
                  () ->
                      new InvalidStateException(
                          ResponseCode.INVALID_STATE, "SSE channel is disabled in configuration"))
              .send(new SseRequest(notification.getRecipient(), notification.getMessageBody()));
      case WHATSAPP ->
          whatsAppChannelService
              .orElseThrow(
                  () ->
                      new InvalidStateException(
                          ResponseCode.INVALID_STATE,
                          "WhatsApp channel is disabled in configuration"))
              .send(notification.getRecipient(), variables);
    };
  }
}
