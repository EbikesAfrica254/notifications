package com.ebikes.notifications.dtos.requests.filters;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.notifications.enums.DeliveryStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DeliveryFilter extends BaseFilter {
  private Integer attemptNumber;
  private OffsetDateTime attemptedAtFrom;
  private OffsetDateTime attemptedAtTo;
  private UUID notificationId;
  private DeliveryStatus status;
}
