package com.ebikes.notifications.dtos.requests.filters;

import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChannelPreferenceFilter extends BaseFilter {

  private NotificationCategory category;
  private ChannelType channel;
  private Boolean enabled;
}
