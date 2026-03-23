package com.ebikes.notifications.constants;

import com.ebikes.notifications.support.references.ReferenceGenerator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventConstants {

  public static final class EventTypes {

    private EventTypes() {
      // prevent instantiation
    }

    public static final class Notifications {

      private Notifications() {
        // prevent instantiation
      }

      public static final String CANCELLED = EventSource.HOST_SERVICE + ".notification.cancelled";
      public static final String DELIVERED = EventSource.HOST_SERVICE + ".notification.delivered";
      public static final String FAILED = EventSource.HOST_SERVICE + ".notification.failed";
      public static final String SUPPRESSED = EventSource.HOST_SERVICE + ".notification.suppressed";
    }

    public static final class Preferences {

      private Preferences() {
        // prevent instantiation
      }

      public static final class Organization {

        private Organization() {
          throw new UnsupportedOperationException(
              ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
        }

        public static final String CREATED =
            EventSource.HOST_SERVICE + ".organization-preference.created";
        public static final String DELETED =
            EventSource.HOST_SERVICE + ".organization-preference.deleted";
        public static final String UPDATED =
            EventSource.HOST_SERVICE + ".organization-preference.updated";
      }

      public static final class User {

        private User() {
          throw new UnsupportedOperationException(
              ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
        }

        public static final String CREATED = EventSource.HOST_SERVICE + ".user-preference.created";
        public static final String DELETED = EventSource.HOST_SERVICE + ".user-preference.deleted";
        public static final String UPDATED = EventSource.HOST_SERVICE + ".user-preference.updated";
      }
    }

    public static final class Templates {

      private Templates() {
        // prevent instantiation
      }

      public static final String ACTIVATED = EventSource.HOST_SERVICE + ".template.activated";
      public static final String CREATED = EventSource.HOST_SERVICE + ".template.created";
      public static final String DEACTIVATED = EventSource.HOST_SERVICE + ".template.deactivated";
      public static final String UPDATED = EventSource.HOST_SERVICE + ".template.updated";
    }
  }

  public static final class EventSource {

    private EventSource() {
      // prevent instantiation
    }

    public static final String HOST_SERVICE = "notifications";

    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(HOST_SERVICE);
    }
  }

  public static final class InboxSourceContext {

    private InboxSourceContext() {
      // prevent instantiation
    }

    private static final String DELIMITER = ":";

    public static final String IAM_SERVICE = "iam";

    public static String getSourceContext(String serviceReference) {
      return serviceReference.split(DELIMITER)[0];
    }
  }

  public static final class MessageHeaders {

    private MessageHeaders() {
      // prevent instantiation
    }

    public static final String EVENT_TYPE = "eventType";
    public static final String OUTBOX_ID = "outboxId";
    public static final String ROUTING_KEY = "routingKey";
  }

  public static final class RoutingKeys {

    private RoutingKeys() {
      // prevent instantiation
    }

    private static final String IAM_HOST_SERVICE = "iam";
    private static final String ORGANIZATIONS_HOST_SERVICE = "organizations";

    // outbound routing keys — pattern: <service>.<domain>.audit → matched by *.*.audit
    public static final String NOTIFICATIONS_NOTIFICATION_AUDIT =
        audit(EventSource.HOST_SERVICE + ".notification");
    public static final String NOTIFICATIONS_PREFERENCE_AUDIT =
        audit(EventSource.HOST_SERVICE + ".preference");
    public static final String NOTIFICATIONS_TEMPLATE_AUDIT =
        audit(EventSource.HOST_SERVICE + ".template");

    // inbound routing keys - external contracts, hardcoded intentionally
    public static final String ORGANIZATION_CONFIGURATION =
        configuration(ORGANIZATIONS_HOST_SERVICE + ".organization");
    public static final String USER_CONFIGURATION = configuration(IAM_HOST_SERVICE + ".user");

    public static String audit(String domain) {
      return domain + ".audit";
    }

    public static String configuration(String domain) {
      return domain + ".configuration";
    }

    public static String notifications(String channel) {
      return "notifications." + channel.toLowerCase();
    }
  }
}
