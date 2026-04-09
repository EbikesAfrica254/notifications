package com.ebikes.notifications.constants;

import com.ebikes.notifications.support.references.ReferenceGenerator;

public final class EventConstants {
  private EventConstants() {
    // prevent instantiation
  }

  public static final class Source {
    private Source() {
      // prevent instantiation
    }

    public static final String HOST_SERVICE = "notifications";

    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(HOST_SERVICE);
    }
  }

  public static final class DomainEvents {
    private DomainEvents() {
      // prevent instantiation
    }

    public static final class Deliveries {
      private Deliveries() {
        // prevent instantiation
      }

      public static final String ATTEMPTED = Source.HOST_SERVICE + ".delivery.attempted";
      public static final String CHANNEL_DISABLED =
          Source.HOST_SERVICE + ".delivery.channel-disabled";
      public static final String DELIVERED = Source.HOST_SERVICE + ".delivery.delivered";
      public static final String FAILED = Source.HOST_SERVICE + ".delivery.failed";
      public static final String INVALID_RECIPIENT =
          Source.HOST_SERVICE + ".delivery.invalid-recipient";
      public static final String OFFLINE = Source.HOST_SERVICE + ".delivery.offline";
      public static final String RATE_LIMITED = Source.HOST_SERVICE + ".delivery.rate-limited";
      public static final String TIMEOUT = Source.HOST_SERVICE + ".delivery.timeout";
    }

    public static final class Notifications {
      private Notifications() {
        // prevent instantiation
      }

      public static final String CANCELLED = Source.HOST_SERVICE + ".notification.cancelled";
      public static final String CREATED = Source.HOST_SERVICE + ".notification.created";
      public static final String DELIVERED = Source.HOST_SERVICE + ".notification.delivered";
      public static final String FAILED = Source.HOST_SERVICE + ".notification.failed";
    }

    public static final class Preferences {
      private Preferences() {
        // prevent instantiation
      }

      public static final class Organization {
        private Organization() {
          // prevent instantiation
        }

        public static final String CREATED =
            Source.HOST_SERVICE + ".organization-preference.created";
        public static final String DELETED =
            Source.HOST_SERVICE + ".organization-preference.deleted";
        public static final String UPDATED =
            Source.HOST_SERVICE + ".organization-preference.updated";
      }

      public static final class User {
        private User() {
          // prevent instantiation
        }

        public static final String CREATED = Source.HOST_SERVICE + ".user-preference.created";
        public static final String DELETED = Source.HOST_SERVICE + ".user-preference.deleted";
        public static final String UPDATED = Source.HOST_SERVICE + ".user-preference.updated";
      }
    }

    public static final class Templates {
      private Templates() {
        // prevent instantiation
      }

      public static final String ACTIVATED = Source.HOST_SERVICE + ".template.activated";
      public static final String CREATED = Source.HOST_SERVICE + ".template.created";
      public static final String DEACTIVATED = Source.HOST_SERVICE + ".template.deactivated";
      public static final String UPDATED = Source.HOST_SERVICE + ".template.updated";
    }
  }

  public static final class ExternalContracts {
    private ExternalContracts() {
      // prevent instantiation
    }

    public static final String ORGANIZATION_CONFIGURATION = "organizations.organization";
    public static final String USER_CONFIGURATION = "notifications.user";
  }
}
