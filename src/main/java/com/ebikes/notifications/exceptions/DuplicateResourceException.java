package com.ebikes.notifications.exceptions;

import java.io.Serial;

import com.ebikes.notifications.enums.ResponseCode;

public class DuplicateResourceException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public DuplicateResourceException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }
}
