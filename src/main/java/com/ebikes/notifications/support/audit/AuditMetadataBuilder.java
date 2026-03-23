package com.ebikes.notifications.support.audit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.entities.OrganizationChannelPreference;
import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.database.entities.UserChannelPreference;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditMetadataBuilder {

  private static final String CATEGORY = "category";
  private static final String CHANNEL = "channel";
  private static final String ENABLED = "enabled";
  private static final String NAME = "name";
  private static final String ORGANIZATION_ID = "organizationId";
  private static final String SERVICE_REFERENCE = "serviceReference";
  private static final String USER_ID = "userId";
  private static final String VERSION = "version";

  public static Map<String, String> forNotification(Notification notification) {
    return Map.of(
        SERVICE_REFERENCE, notification.getServiceReference(),
        CHANNEL, notification.getChannel().name(),
        ORGANIZATION_ID, notification.getOrganizationId());
  }

  public static Map<String, String> forNotification(
      Notification notification, Map<String, String> extra) {
    Map<String, String> metadata = new HashMap<>(forNotification(notification));
    metadata.putAll(extra);
    return Collections.unmodifiableMap(metadata);
  }

  public static Map<String, String> forOrganizationChannelPreference(
      OrganizationChannelPreference preference) {
    return Map.of(
        CHANNEL, preference.getChannel().name(),
        CATEGORY, preference.getCategory().name(),
        ENABLED, String.valueOf(preference.getEnabled()));
  }

  public static Map<String, String> forTemplate(Template template) {
    return Map.of(
        NAME, template.getName(),
        CHANNEL, String.valueOf(template.getChannel()),
        VERSION, String.valueOf(template.getVersion()));
  }

  public static Map<String, String> forUserChannelPreference(UserChannelPreference preference) {
    return Map.of(
        USER_ID, preference.getUserId(),
        CHANNEL, preference.getChannel().name(),
        CATEGORY, preference.getCategory().name(),
        ENABLED, String.valueOf(preference.getEnabled()));
  }
}
