package com.ebikes.notifications.constants;

import java.util.Set;

import com.ebikes.notifications.enums.NotificationCategory;

public final class PreferenceConstants {

  public static final Set<NotificationCategory> MANDATORY_CATEGORIES =
      Set.of(NotificationCategory.SECURITY, NotificationCategory.TRANSACTIONAL);

  private PreferenceConstants() {}
}
