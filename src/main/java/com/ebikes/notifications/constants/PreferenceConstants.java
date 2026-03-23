package com.ebikes.notifications.constants;

import java.util.Set;

import com.ebikes.notifications.enums.NotificationCategory;

/**
 * Shared constants for the preference subsystem.
 *
 * <p>Centralises {@code MANDATORY_CATEGORIES} so that both {@code OrganizationPreferenceService}
 * and {@code UserPreferenceService} reference a single source of truth. Any future change to which
 * categories are mandatory only requires editing this class.
 */
public final class PreferenceConstants {

  // Fix #6: Extracted from both service classes to eliminate duplication.
  public static final Set<NotificationCategory> MANDATORY_CATEGORIES =
      Set.of(NotificationCategory.SECURITY, NotificationCategory.TRANSACTIONAL);

  private PreferenceConstants() {}
}
