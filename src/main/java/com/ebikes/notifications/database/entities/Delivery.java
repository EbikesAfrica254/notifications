package com.ebikes.notifications.database.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Type;

import com.ebikes.notifications.database.entities.bases.BaseEntity;
import com.ebikes.notifications.enums.DeliveryStatus;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.support.audit.Auditable;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@AttributeOverride(
    name = "createdAt",
    column =
        @Column(
            name = "attempted_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMPTZ"))
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "deliveries",
    schema = "notifications",
    indexes = {
      @Index(
          name = "idx_deliveries_notification_seq",
          columnList = "notification_id, attempt_number"),
      @Index(
          name = "idx_deliveries_organization_seq",
          columnList = "organization_id, notification_id"),
      @Index(name = "idx_deliveries_retry_queue", columnList = "status, next_retry_at"),
      @Index(name = "idx_deliveries_time", columnList = "attempted_at")
    })
@ToString(exclude = {"notification", "providerResponse", "errorMessage"})
public class Delivery extends BaseEntity implements Auditable {

  @Column(name = "attempt_number", nullable = false, updatable = false)
  private Integer attemptNumber;

  @Column(name = "completed_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime completedAt;

  @Column(name = "cost_amount", precision = 10, scale = 4)
  private BigDecimal costAmount;

  @Column(name = "cost_currency", length = 3)
  private String costCurrency;

  @Column(name = "error_code", length = 100)
  private String errorCode;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @JoinColumn(name = "notification_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Notification notification;

  @Column(name = "organization_id", nullable = false, updatable = false, length = 36)
  private String organizationId;

  @Column(name = "provider_message_id")
  private String providerMessageId;

  @Column(name = "provider_response", columnDefinition = "JSONB")
  @Type(JsonBinaryType.class)
  private Map<String, Serializable> providerResponse;

  @Column(name = "status", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private DeliveryStatus status;

  public void markDelivered(
      BigDecimal costAmount,
      String costCurrency,
      String providerMessageId,
      Map<String, Serializable> providerResponse) {
    validateTransitionFromPending("DELIVERED");
    this.status = DeliveryStatus.DELIVERED;
    this.providerMessageId = providerMessageId;
    this.providerResponse = providerResponse;
    this.costAmount = costAmount;
    this.costCurrency = costCurrency;
    this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void markChannelDisabled() {
    validateTransitionFromPending("CHANNEL_DISABLED");
    this.status = DeliveryStatus.CHANNEL_DISABLED;
    this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void markFailed(
      String errorCode, String errorMessage, Map<String, Serializable> providerResponse) {
    validateTransitionFromPending("FAILED");
    this.status = DeliveryStatus.FAILED;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.providerResponse = providerResponse;
    this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void markInvalidRecipient(String errorCode, String errorMessage) {
    validateTransitionFromPending("INVALID_RECIPIENT");
    this.status = DeliveryStatus.INVALID_RECIPIENT;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void markOffline() {
    validateTransitionFromPending("OFFLINE");
    this.status = DeliveryStatus.OFFLINE;
    this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void markRateLimited() {
    validateTransitionFromPending("RATE_LIMITED");
    this.status = DeliveryStatus.RATE_LIMITED;
    this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void markTimeout() {
    validateTransitionFromPending("TIMEOUT");
    this.status = DeliveryStatus.TIMEOUT;
    this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    return Map.of(
        "attemptNumber", String.valueOf(attemptNumber),
        "organizationId", organizationId,
        "status", status.name());
  }

  private void validateTransitionFromPending(String targetStatus) {
    if (this.status != DeliveryStatus.PENDING) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE,
          "Cannot transition delivery to " + targetStatus + " from status=" + this.status);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Delivery delivery = (Delivery) o;
    UUID id = getId();
    return id != null && id.equals(delivery.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
