package com.ebikes.notifications.support.fixtures;

import com.ebikes.notifications.database.entities.OrganizationChannelPreference;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;

public final class OrganizationPreferenceFixtures {

  private OrganizationPreferenceFixtures() {}

  public static OrganizationChannelPreference enabled(
      String organizationId, ChannelType channel, NotificationCategory category) {
    return base(organizationId, channel, category, true).build();
  }

  public static OrganizationChannelPreference disabled(
      String organizationId, ChannelType channel, NotificationCategory category) {
    return base(organizationId, channel, category, false).build();
  }

  private static OrganizationChannelPreference.OrganizationChannelPreferenceBuilder<?, ?> base(
      String organizationId, ChannelType channel, NotificationCategory category, boolean enabled) {
    return OrganizationChannelPreference.builder()
        .organizationId(organizationId)
        .channel(channel)
        .category(category)
        .enabled(enabled);
  }
}
