package com.ebikes.notifications.dtos.requests.preferences.user;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;

public record CreateUserPreferenceRequest(
    @NotNull(message = "Category is required") NotificationCategory category,
    @NotNull(message = "Channel is required") ChannelType channel,
    Boolean enabled)
    implements Serializable {

  public CreateUserPreferenceRequest {
    enabled = enabled != null ? enabled : Boolean.TRUE;
  }
}
