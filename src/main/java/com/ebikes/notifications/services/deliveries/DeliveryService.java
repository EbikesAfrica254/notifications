package com.ebikes.notifications.services.deliveries;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.notifications.constants.EventConstants.DomainEvents;
import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.repositories.DeliveryRepository;
import com.ebikes.notifications.database.repositories.NotificationRepository;
import com.ebikes.notifications.database.specifications.AuthorizationSpecifications;
import com.ebikes.notifications.database.specifications.DeliverySpecifications;
import com.ebikes.notifications.dtos.responses.deliveries.DeliveryResponse;
import com.ebikes.notifications.enums.DeliveryStatus;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.DeliveryMapper;
import com.ebikes.notifications.support.audit.AuditTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class DeliveryService {

  private final AuditTemplate auditTemplate;
  private final DeliveryMapper deliveryMapper;
  private final DeliveryRepository deliveryRepository;
  private final NotificationRepository notificationRepository;

  @Transactional
  public Delivery createAttempt(UUID notificationId, Integer attemptNumber) {
    Notification notification = findNotificationById(notificationId);

    Delivery delivery =
        Delivery.builder()
            .attemptNumber(attemptNumber)
            .notification(notification)
            .organizationId(notification.getOrganizationId())
            .status(DeliveryStatus.PENDING)
            .build();

    return auditTemplate.execute(
        delivery,
        notification.getOrganizationId(),
        DomainEvents.Deliveries.ATTEMPTED,
        () -> {
          Delivery saved = deliveryRepository.save(delivery);
          log.info(
              "Delivery attempt created - notificationId={} attemptNumber={}",
              notificationId,
              attemptNumber);
          return saved;
        });
  }

  public Delivery findById(UUID deliveryId) {
    return deliveryRepository
        .findById(deliveryId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Delivery not found"));
  }

  public List<DeliveryResponse> findByNotificationId(UUID notificationId) {
    Specification<Delivery> spec =
        AuthorizationSpecifications.forDeliveries()
            .and(DeliverySpecifications.forNotification(notificationId));

    return deliveryRepository
        .findAll(spec, Sort.by(Sort.Direction.ASC, DeliverySpecifications.FIELD_ATTEMPT_NUMBER))
        .stream()
        .map(deliveryMapper::toResponse)
        .toList();
  }

  @Transactional
  public void recordDelivered(
      BigDecimal costAmount,
      String costCurrency,
      UUID deliveryId,
      String providerMessageId,
      Map<String, Serializable> providerResponse) {

    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markDelivered(costAmount, costCurrency, providerMessageId, providerResponse);

    auditTemplate.execute(
        delivery,
        delivery.getOrganizationId(),
        DomainEvents.Deliveries.DELIVERED,
        () -> {
          deliveryRepository.save(delivery);
          log.info(
              "Delivery succeeded - deliveryId={} notificationId={} attemptNumber={}",
              deliveryId,
              delivery.getNotification().getId(),
              delivery.getAttemptNumber());
        });
  }

  @Transactional
  public void recordChannelDisabled(UUID deliveryId) {
    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markChannelDisabled();

    auditTemplate.execute(
        delivery,
        delivery.getOrganizationId(),
        DomainEvents.Deliveries.CHANNEL_DISABLED,
        () -> {
          deliveryRepository.save(delivery);
          log.warn(
              "Channel disabled - deliveryId={} notificationId={} attemptNumber={}",
              deliveryId,
              delivery.getNotification().getId(),
              delivery.getAttemptNumber());
        });
  }

  @Transactional
  public void recordFailed(
      UUID deliveryId,
      String errorCode,
      String errorMessage,
      Map<String, Serializable> providerResponse) {

    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markFailed(errorCode, errorMessage, providerResponse);

    auditTemplate.execute(
        delivery,
        delivery.getOrganizationId(),
        DomainEvents.Deliveries.FAILED,
        () -> {
          deliveryRepository.save(delivery);
          log.warn(
              "Delivery failed - deliveryId={} notificationId={} attemptNumber={} errorCode={}",
              deliveryId,
              delivery.getNotification().getId(),
              delivery.getAttemptNumber(),
              errorCode);
        });
  }

  @Transactional
  public void recordInvalidRecipient(UUID deliveryId, String errorCode, String errorMessage) {
    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markInvalidRecipient(errorCode, errorMessage);

    auditTemplate.execute(
        delivery,
        delivery.getOrganizationId(),
        DomainEvents.Deliveries.INVALID_RECIPIENT,
        () -> {
          deliveryRepository.save(delivery);
          log.warn(
              "Invalid recipient - deliveryId={} notificationId={} attemptNumber={}",
              deliveryId,
              delivery.getNotification().getId(),
              delivery.getAttemptNumber());
        });
  }

  @Transactional
  public void recordOffline(UUID deliveryId) {
    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markOffline();

    auditTemplate.execute(
        delivery,
        delivery.getOrganizationId(),
        DomainEvents.Deliveries.OFFLINE,
        () -> {
          deliveryRepository.save(delivery);
          log.debug(
              "SSE recipient offline - deliveryId={} notificationId={} attemptNumber={}",
              deliveryId,
              delivery.getNotification().getId(),
              delivery.getAttemptNumber());
        });
  }

  @Transactional
  public void recordRateLimited(UUID deliveryId) {
    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markRateLimited();

    auditTemplate.execute(
        delivery,
        delivery.getOrganizationId(),
        DomainEvents.Deliveries.RATE_LIMITED,
        () -> deliveryRepository.save(delivery));
  }

  @Transactional
  public void recordTimeout(UUID deliveryId) {
    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markTimeout();

    auditTemplate.execute(
        delivery,
        delivery.getOrganizationId(),
        DomainEvents.Deliveries.TIMEOUT,
        () -> {
          deliveryRepository.save(delivery);
          log.warn(
              "Delivery timed out - deliveryId={} notificationId={} attemptNumber={}",
              deliveryId,
              delivery.getNotification().getId(),
              delivery.getAttemptNumber());
        });
  }

  private Delivery findDeliveryById(UUID deliveryId) {
    return deliveryRepository
        .findById(deliveryId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Delivery with id " + deliveryId + " not found"));
  }

  private Notification findNotificationById(UUID notificationId) {
    return notificationRepository
        .findById(notificationId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Notification with id " + notificationId + " not found"));
  }
}
