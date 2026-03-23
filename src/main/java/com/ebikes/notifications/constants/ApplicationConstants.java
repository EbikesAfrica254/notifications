package com.ebikes.notifications.constants;

public final class ApplicationConstants {
  public static final String CLASS_CANNOT_BE_INSTANTIATED = "Class cannot be instantiated";
  public static final String DOCUMENTATION_ERRORS_BASE = "https://docs.ebikesafrica.co.ke/errors/";
  public static final String ERROR_REFERENCE_PREFIX = "ERR";
  public static final int ERROR_REFERENCE_ID_LENGTH = 6;
  public static final String MESSAGE_REFERENCE_PREFIX = "MSG";
  public static final int MESSAGE_REFERENCE_ID_LENGTH = 8;
  public static final String PROBLEM_JSON_MEDIA_TYPE = "application/problem+json";
  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String SYSTEM_ID = "00000000-0000-0000-0000-000000000000";

  public static final class HttpClient {
    public static final int CONNECT_TIMEOUT_SECONDS = 10;
    public static final int READ_TIMEOUT_SECONDS = 15;
    public static final int WHATSAPP_CONNECT_TIMEOUT_SECONDS = 5;
    public static final int WHATSAPP_READ_TIMEOUT_SECONDS = 10;

    private HttpClient() {
      throw new UnsupportedOperationException(CLASS_CANNOT_BE_INSTANTIATED);
    }
  }

  public static final class Outbox {
    public static final String BINDING_NAME = "eventPublisher-out-0";
    public static final int MAX_RETRY_COUNT = 5;

    private Outbox() {
      throw new UnsupportedOperationException(CLASS_CANNOT_BE_INSTANTIATED);
    }
  }

  public static final class WhatsApp {
    public static final String BASE_URL = "https://graph.facebook.com/v19.0";
    public static final String ENDPOINT_PREFIX = "whatsapp://";
    public static final int RATE_LIMIT_APP = 4;
    public static final int RATE_LIMIT_BUSINESS = 130429;
    public static final int RATE_LIMIT_PER_USER = 131056;
    public static final int AUTH_ERROR_CODE = 190;
    public static final String AUTH_EXCEPTION_TYPE = "OAuthException";

    // Message types
    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_INTERACTIVE = "interactive";
    public static final String MESSAGE_TYPE_DOCUMENT = "document";

    // Interactive subtypes
    public static final String INTERACTIVE_TYPE_BUTTON = "button";
    public static final String INTERACTIVE_TYPE_LIST = "list";

    // Button reply type
    public static final String BUTTON_REPLY_TYPE = "reply";

    // Messaging product
    public static final String MESSAGING_PRODUCT = "whatsapp";

    private WhatsApp() {
      throw new UnsupportedOperationException(CLASS_CANNOT_BE_INSTANTIATED);
    }
  }

  public static final class TaifaMobile {
    public static final String BASE_URL = "https://api.taifamobile.co.ke";
    public static final String ENDPOINT_PREFIX = "taifamobile://";
    public static final String SEND_SMS_PATH = "/sms/sendsms";
    public static final String STATUS_AUTH_INVALID_SENDER = "1001";
    public static final String STATUS_AUTH_INVALID_API_KEY = "1002";
    public static final String STATUS_AUTH_ACCOUNT_INACTIVE = "1006";
    public static final String STATUS_LOW_CREDITS = "1004";
    public static final String STATUS_AUTH_IP_NOT_WHITELISTED = "1013";
    public static final String STATUS_AUTH_INVALID_ACCOUNT = "1014";

    private TaifaMobile() {
      throw new UnsupportedOperationException(CLASS_CANNOT_BE_INSTANTIATED);
    }
  }

  private ApplicationConstants() {
    throw new UnsupportedOperationException(CLASS_CANNOT_BE_INSTANTIATED);
  }
}
