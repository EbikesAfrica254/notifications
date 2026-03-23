package com.ebikes.notifications.exceptions;

import java.io.Serial;

import com.ebikes.notifications.enums.ResponseCode;

public class RateLimitException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public RateLimitException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }

  public RateLimitException(ResponseCode code, String developerMessage, Throwable cause) {
    super(code, developerMessage, cause);
  }
}
