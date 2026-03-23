package com.ebikes.notifications.exceptions;

import java.io.Serial;

import com.ebikes.notifications.enums.ResponseCode;

public class TemplateException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public TemplateException(ResponseCode responseCode, String developerMessage) {
    super(responseCode, developerMessage);
  }

  public TemplateException(ResponseCode responseCode, String developerMessage, Throwable cause) {
    super(responseCode, developerMessage, cause);
  }
}
