package com.ebikes.notifications.database.entities;

import java.io.Serializable;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.Type;

import com.ebikes.notifications.database.entities.bases.AuditableEntity;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationStatus;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.InvalidStateException;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "notifications",
    schema = "notifications",
    indexes = {
      @Index(name = "idx_notifications_branch_id", columnList = "branch_id"),
      @Index(name = "idx_notifications_organization_id", columnList = "organization_id"),
      @Index(name = "idx_notifications_recipient", columnList = "recipient"),
      @Index(name = "idx_notifications_service_reference", columnList = "service_reference"),
      @Index(name = "idx_notifications_status", columnList = "status")
    })
public class Notification extends AuditableEntity {

  @Column(name = "branch_id", updatable = false)
  private String branchId;

  @Column(name = "channel", nullable = false, updatable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull(message = "Channel is required") private ChannelType channel;

  @Column(name = "message_body", nullable = false, updatable = false, columnDefinition = "TEXT")
  @NotBlank(message = "Message body is required") private String messageBody;

  @Column(name = "message_subject", updatable = false, length = 500)
  @Size(max = 500, message = "Message subject must not exceed 500 characters") private String messageSubject;

  @Column(name = "organization_id", nullable = false, updatable = false)
  @NotNull(message = "Organization ID is required") private String organizationId;

  @Column(name = "recipient", nullable = false, updatable = false)
  @NotBlank(message = "Recipient is required") private String recipient;

  @Column(
      name = "service_reference",
      nullable = false,
      unique = true,
      updatable = false,
      length = 100)
  @NotBlank(message = "Service reference is required") private String serviceReference;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull(message = "Status is required") private NotificationStatus status;

  @JoinColumn(name = "template_id", updatable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Template template;

  @Column(name = "template_version", updatable = false)
  private Integer templateVersion;

  @Column(name = "variables", columnDefinition = "JSONB")
  @Type(JsonBinaryType.class)
  private Map<String, Serializable> variables;

  @Builder
  public Notification(
      String branchId,
      @NotNull ChannelType channel,
      @NotBlank String messageBody,
      String messageSubject,
      @NotNull String organizationId,
      @NotBlank String recipient,
      @NotBlank String serviceReference,
      Template template,
      Integer templateVersion,
      Map<String, Serializable> variables) {

    this.branchId = branchId;
    this.channel = channel;
    this.messageBody = messageBody;
    this.messageSubject = messageSubject;
    this.organizationId = organizationId;
    this.recipient = recipient;
    this.serviceReference = serviceReference;
    this.status = NotificationStatus.PENDING;
    this.template = template;
    this.templateVersion = templateVersion;
    this.variables = variables;
  }

  public void cancel() {
    if (this.status != NotificationStatus.PENDING && this.status != NotificationStatus.PROCESSING) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE, "Cannot cancel notification with status=" + this.status);
    }
    this.status = NotificationStatus.CANCELLED;
  }

  public void markDelivered() {
    if (this.status != NotificationStatus.PROCESSING) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE,
          "Cannot mark notification as DELIVERED from status=" + this.status);
    }
    this.status = NotificationStatus.DELIVERED;
  }

  public void markFailed() {
    if (this.status != NotificationStatus.PROCESSING) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE,
          "Cannot mark notification as FAILED from status=" + this.status);
    }
    this.status = NotificationStatus.FAILED;
  }

  public void markProcessing() {
    if (this.status != NotificationStatus.PENDING) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE,
          "Cannot mark notification as PROCESSING from status=" + this.status);
    }
    this.status = NotificationStatus.PROCESSING;
  }

  public void scheduleRetry() {
    if (this.status != NotificationStatus.FAILED) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE,
          "Cannot schedule retry for notification with status=" + this.status);
    }
    this.status = NotificationStatus.PENDING;
  }

  @AssertTrue(
      message = "Subject is required for EMAIL channel and must be null for all other channels")
  private boolean isSubjectValidForChannel() {
    if (channel == null) {
      return true;
    }
    if (channel == ChannelType.EMAIL) {
      return messageSubject != null && !messageSubject.isBlank();
    }
    return messageSubject == null;
  }
}
