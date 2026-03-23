package com.ebikes.notifications.enums;

import jakarta.validation.constraints.NotBlank;

public enum WhatsAppMessageType {
  BUTTONS,
  DOCUMENT,
  LIST,
  TEXT;

  public static WhatsAppMessageType fromString(@NotBlank String value) {
    try {
      return WhatsAppMessageType.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid WhatsApp messageType: " + value);
    }
  }
}
