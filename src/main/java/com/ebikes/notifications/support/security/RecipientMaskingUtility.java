package com.ebikes.notifications.support.security;

public class RecipientMaskingUtility {

  private static final int EMAIL_VISIBLE_PREFIX = 1;
  private static final int PHONE_VISIBLE_SUFFIX = 3;
  private static final String MASK_SEGMENT = "•••••";

  private RecipientMaskingUtility() {
    // prevent instantiation
  }

  public static String mask(String recipient) {
    if (recipient == null || recipient.isBlank()) {
      return recipient;
    }
    return recipient.contains("@") ? maskEmail(recipient) : maskPhone(recipient);
  }

  private static String maskEmail(String email) {
    int atIndex = email.indexOf('@');
    if (atIndex <= EMAIL_VISIBLE_PREFIX) {
      return MASK_SEGMENT + email.substring(atIndex);
    }
    return email.charAt(0) + MASK_SEGMENT + email.substring(atIndex);
  }

  private static String maskPhone(String phone) {
    if (phone.length() <= PHONE_VISIBLE_SUFFIX) {
      return MASK_SEGMENT;
    }
    String suffix = phone.substring(phone.length() - PHONE_VISIBLE_SUFFIX);
    String prefix = phone.substring(0, Math.min(4, phone.length() - PHONE_VISIBLE_SUFFIX));
    return prefix + MASK_SEGMENT + suffix;
  }
}
