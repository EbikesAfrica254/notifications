package com.ebikes.notifications.dtos.requests.filters;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class NotificationFilter extends BaseFilter {
  private String branchId;
  private ChannelType channel;
  private OffsetDateTime createdAtFrom;
  private OffsetDateTime createdAtTo;
  private String organizationId;
  private String recipient;
  private NotificationStatus status;
  private UUID templateId;
}
