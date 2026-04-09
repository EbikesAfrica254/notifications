package com.ebikes.notifications.database.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
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
import com.ebikes.notifications.support.audit.Auditable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "organization_channel_preferences",
    schema = "notifications",
    indexes = {
      @Index(name = "idx_org_prefs_lookup", columnList = "organization_id, channel, category")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_org_channel_category",
          columnNames = {"organization_id", "channel", "category"})
    })
@ToString
public class OrganizationChannelPreference extends BaseEntity implements Auditable {

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

  @Column(name = "version", nullable = false)
  @Version
  private Integer version;

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
  public Map<String, String> toAuditMetadata() {
    return Map.of(
        "category", category.name(),
        "channel", channel.name(),
        "organizationId", organizationId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrganizationChannelPreference that = (OrganizationChannelPreference) o;
    return organizationId != null
        && organizationId.equals(that.organizationId)
        && channel == that.channel
        && category == that.category;
  }

  @Override
  public int hashCode() {
    return Objects.hash(organizationId, channel, category);
  }
}
