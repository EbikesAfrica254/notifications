package com.ebikes.notifications.exceptions;

import java.io.Serial;

import com.ebikes.notifications.enums.ResponseCode;

public class AuthorizationException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public AuthorizationException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }

  public AuthorizationException(ResponseCode code, String developerMessage, Throwable cause) {
    super(code, developerMessage, cause);
  }
}
