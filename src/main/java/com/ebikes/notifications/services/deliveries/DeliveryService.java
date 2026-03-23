package com.ebikes.notifications.services.deliveries;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.repositories.DeliveryRepository;
import com.ebikes.notifications.database.repositories.NotificationRepository;
import com.ebikes.notifications.database.specifications.AuthorizationSpecifications;
import com.ebikes.notifications.database.specifications.DeliverySpecifications;
import com.ebikes.notifications.dtos.responses.deliveries.DeliveryResponse;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.DeliveryMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class DeliveryService {

  private static final int BASE_BACKOFF_SECONDS = 60;
  private static final int MAX_BACKOFF_SECONDS = 3600;

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
            .build();

    Delivery savedDelivery = deliveryRepository.save(delivery);

    log.info(
        "Delivery attempt created - notificationId={} attemptNumber={}",
        notificationId,
        attemptNumber);

    return savedDelivery;
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
    deliveryRepository.save(delivery);

    log.info(
        "Delivery succeeded - deliveryId={} notificationId={} attemptNumber={}",
        deliveryId,
        delivery.getNotification().getId(),
        delivery.getAttemptNumber());
  }

  @Transactional
  public void recordChannelDisabled(UUID deliveryId) {
    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markChannelDisabled();
    deliveryRepository.save(delivery);

    log.warn(
        "Channel disabled - deliveryId={} notificationId={} attemptNumber={}",
        deliveryId,
        delivery.getNotification().getId(),
        delivery.getAttemptNumber());
  }

  @Transactional
  public void recordFailed(
      UUID deliveryId,
      String errorCode,
      String errorMessage,
      Map<String, Serializable> providerResponse,
      OffsetDateTime nextRetryAt) {

    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markFailed(errorCode, errorMessage, providerResponse, nextRetryAt);
    deliveryRepository.save(delivery);

    log.warn(
        "Delivery failed - deliveryId={} notificationId={} attemptNumber={} errorCode={}",
        deliveryId,
        delivery.getNotification().getId(),
        delivery.getAttemptNumber(),
        errorCode);
  }

  @Transactional
  public void recordInvalidRecipient(UUID deliveryId, String errorCode, String errorMessage) {

    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markInvalidRecipient(errorCode, errorMessage);
    deliveryRepository.save(delivery);

    log.warn(
        "Invalid recipient - deliveryId={} notificationId={} attemptNumber={}",
        deliveryId,
        delivery.getNotification().getId(),
        delivery.getAttemptNumber());
  }

  @Transactional
  public void recordRateLimited(UUID deliveryId, Integer attemptNumber) {
    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markRateLimited(calculateNextRetryAt(attemptNumber));
    deliveryRepository.save(delivery);
  }

  @Transactional
  public void recordTimeout(UUID deliveryId, Integer attemptNumber) {
    Delivery delivery = findDeliveryById(deliveryId);
    delivery.markTimeout(calculateNextRetryAt(attemptNumber));
    deliveryRepository.save(delivery);

    log.warn(
        "Delivery timed out - deliveryId={} notificationId={} attemptNumber={} nextRetryAt={}",
        deliveryId,
        delivery.getNotification().getId(),
        delivery.getAttemptNumber(),
        delivery.getNextRetryAt());
  }

  private OffsetDateTime calculateNextRetryAt(Integer attemptNumber) {
    int delaySeconds = BASE_BACKOFF_SECONDS * (int) Math.pow(2, attemptNumber - 1.0);
    delaySeconds = Math.min(delaySeconds, MAX_BACKOFF_SECONDS);
    return OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds);
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
