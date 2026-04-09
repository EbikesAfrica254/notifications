package com.ebikes.notifications.services.notifications;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.exceptions.RateLimitException;
import com.ebikes.notifications.exceptions.RecipientOfflineException;
import com.ebikes.notifications.services.channels.ChannelService;
import com.ebikes.notifications.services.deliveries.DeliveryService;
import com.ebikes.notifications.services.preferences.UserPreferenceService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageProcessor {

  private final Map<ChannelType, ChannelService> channelServices;
  private final DeliveryService deliveryService;
  private final NotificationService notificationService;
  private final UserPreferenceService userPreferenceService;

  public MessageProcessor(
      List<ChannelService> channelServices,
      DeliveryService deliveryService,
      NotificationService notificationService,
      UserPreferenceService userPreferenceService) {
    this.channelServices =
        channelServices.stream()
            .collect(Collectors.toMap(ChannelService::getChannelType, Function.identity()));
    this.deliveryService = deliveryService;
    this.notificationService = notificationService;
    this.userPreferenceService = userPreferenceService;
  }

  public void process(NotificationRequest request) {
    if (!userPreferenceService.isEnabled(request)) {
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
    notificationService.markProcessing(notification.getId());
    Delivery delivery = deliveryService.createAttempt(notification.getId(), 1);

    ChannelService service = channelServices.get(notification.getChannel());

    if (service == null) {
      log.error(
          "No channel services registered for channel={} - serviceReference={}",
          notification.getChannel(),
          request.serviceReference());
      deliveryService.recordChannelDisabled(delivery.getId());
      notificationService.markFailed(notification.getId());
      return;
    }

    try {
      ChannelResponse response = service.send(notification);
      recordDelivered(notification, delivery, response);

    } catch (RecipientOfflineException e) {
      log.debug(
          "SSE recipient offline - scheduling retry - serviceReference={} notificationId={}",
          request.serviceReference(),
          notification.getId());
      deliveryService.recordOffline(delivery.getId());
      notificationService.markFailed(notification.getId());
      notificationService.scheduleRetry(notification.getId());

    } catch (InvalidStateException e) {
      log.error(
          "Failed to process message body - serviceReference={} notificationId={} channel={}"
              + " error={}",
          request.serviceReference(),
          notification.getId(),
          notification.getChannel(),
          e.getMessage());
      deliveryService.recordFailed(
          delivery.getId(), e.getResponseCode().getCode(), e.getMessage(), null);
      notificationService.markFailed(notification.getId());
    } catch (RateLimitException e) {
      log.warn(
          "Delivery rate limited - serviceReference={} notificationId={} error={}",
          request.serviceReference(),
          notification.getId(),
          e.getMessage());
      deliveryService.recordRateLimited(delivery.getId());
      notificationService.scheduleRetry(notification.getId());
      throw e;

    } catch (ExternalServiceException e) {
      log.error(
          "Channel delivery failed - serviceReference={} notificationId={} error={}",
          request.serviceReference(),
          notification.getId(),
          e.getMessage(),
          e);
      if (e.getResponseCode() == ResponseCode.INVALID_RECIPIENT) {
        deliveryService.recordInvalidRecipient(
            delivery.getId(), e.getResponseCode().getCode(), e.getMessage());
      } else {
        deliveryService.recordFailed(
            delivery.getId(), e.getResponseCode().getCode(), e.getMessage(), null);
      }
      notificationService.markFailed(notification.getId());
      throw e;

    } catch (Exception e) {
      log.error(
          "Unexpected error processing notification - serviceReference={} notificationId={}",
          request.serviceReference(),
          notification.getId(),
          e);
      deliveryService.recordFailed(delivery.getId(), "PROCESSING_ERROR", e.getMessage(), null);
      notificationService.markFailed(notification.getId());
      throw e;
    }
  }

  private void recordDelivered(
      Notification notification, Delivery delivery, ChannelResponse response) {
    deliveryService.recordDelivered(
        response.costAmount(),
        response.costCurrency(),
        delivery.getId(),
        response.providerMessageId(),
        response.metadata());
    notificationService.markDelivered(notification.getId());
    log.info(
        "Notification delivered - serviceReference={} notificationId={} provider={}",
        notification.getServiceReference(),
        notification.getId(),
        response.providerName());
  }
}
