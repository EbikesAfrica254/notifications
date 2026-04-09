package com.ebikes.notifications.support.fixtures;

import com.ebikes.notifications.database.entities.UserChannelPreference;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;

public final class UserPreferenceFixtures {

  private UserPreferenceFixtures() {}

  public static UserChannelPreference enabled(
      String userId, String organizationId, ChannelType channel, NotificationCategory category) {
    return base(userId, organizationId, channel, category, true).build();
  }

  public static UserChannelPreference disabled(
      String userId, String organizationId, ChannelType channel, NotificationCategory category) {
    return base(userId, organizationId, channel, category, false).build();
  }

  private static UserChannelPreference.UserChannelPreferenceBuilder<?, ?> base(
      String userId,
      String organizationId,
      ChannelType channel,
      NotificationCategory category,
      boolean enabled) {
    return UserChannelPreference.builder()
        .userId(userId)
        .organizationId(organizationId)
        .channel(channel)
        .category(category)
        .enabled(enabled);
  }
}
