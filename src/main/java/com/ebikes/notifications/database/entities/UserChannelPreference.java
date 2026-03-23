package com.ebikes.notifications.database.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import com.ebikes.notifications.database.entities.bases.BaseEntity;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.InvalidStateException;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_channel_preferences",
    schema = "notifications",
    indexes = {
      @Index(
          name = "idx_user_prefs_lookup",
          columnList = "user_id, organization_id, channel, category")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_user_channel_category",
          columnNames = {"user_id", "organization_id", "channel", "category"})
    })
@ToString
public class UserChannelPreference extends BaseEntity {

  @Column(name = "category", nullable = false, updatable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private NotificationCategory category;

  @Column(name = "channel", nullable = false, updatable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private ChannelType channel;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled;

  @Column(name = "organization_id", nullable = false, updatable = false, length = 36)
  private String organizationId;

  @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt;

  @Column(name = "user_id", nullable = false, updatable = false, length = 36)
  private String userId;

  @Column(name = "version", nullable = false)
  @Version
  private Integer version;

  @Builder
  public UserChannelPreference(
      NotificationCategory category,
      ChannelType channel,
      Boolean enabled,
      String organizationId,
      String userId) {

    if (category == null) {
      throw new IllegalArgumentException("Category is required");
    }
    if (channel == null) {
      throw new IllegalArgumentException("Channel is required");
    }
    if (organizationId == null || organizationId.isBlank()) {
      throw new IllegalArgumentException("Organization ID is required");
    }
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("User ID is required");
    }

    this.category = category;
    this.channel = channel;
    this.enabled = enabled != null ? enabled : Boolean.TRUE;
    this.organizationId = organizationId;
    this.userId = userId;
  }

  public void disable() {
    if (Boolean.FALSE.equals(this.enabled)) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE,
          "Channel " + this.channel + " is already disabled for category " + this.category);
    }
    this.enabled = Boolean.FALSE;
  }

  public void enable() {
    if (Boolean.TRUE.equals(this.enabled)) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE,
          "Channel " + this.channel + " is already enabled for category " + this.category);
    }
    this.enabled = Boolean.TRUE;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserChannelPreference that = (UserChannelPreference) o;
    return userId != null
        && userId.equals(that.userId)
        && organizationId != null
        && organizationId.equals(that.organizationId)
        && channel == that.channel
        && category == that.category;
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, organizationId, channel, category);
  }
}
