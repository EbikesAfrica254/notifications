package com.ebikes.notifications.exceptions;

import java.io.Serial;
import java.io.Serializable;

import com.ebikes.notifications.enums.ResponseCode;

import lombok.Getter;

@Getter
public class ValidationException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  private final String field;
  private final Serializable rejectedValue;

  public ValidationException(
      ResponseCode responseCode,
      String developerMessage,
      String field,
      Serializable rejectedValue) {
    super(responseCode, developerMessage);
    this.field = field;
    this.rejectedValue = rejectedValue;
  }
}
