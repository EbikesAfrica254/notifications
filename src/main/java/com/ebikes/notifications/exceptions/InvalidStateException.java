package com.ebikes.notifications.exceptions;

import java.io.Serial;

import com.ebikes.notifications.enums.ResponseCode;

public class InvalidStateException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public InvalidStateException(ResponseCode responseCode, String developerMessage) {
    super(responseCode, developerMessage);
  }
}
