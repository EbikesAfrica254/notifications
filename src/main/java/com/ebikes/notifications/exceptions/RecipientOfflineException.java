package com.ebikes.notifications.exceptions;

import java.io.Serial;
import java.util.UUID;

import lombok.Getter;

@Getter
public class RecipientOfflineException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final UUID notificationId;
  private final String recipient;

  public RecipientOfflineException(UUID notificationId, String recipient) {
    super("Recipient offline - notificationId=" + notificationId + " recipient=" + recipient);
    this.notificationId = notificationId;
    this.recipient = recipient;
  }
}
