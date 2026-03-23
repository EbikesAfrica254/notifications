package com.ebikes.notifications.dtos.requests.channels.sms;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SmsRequest(
    @NotNull(message = "SMS body is required") @Size(min = 1, max = 1600, message = "SMS body must be between 1 and 1600 characters") String body,
    @NotEmpty(message = "At least one recipient is required") List<
                @NotNull(message = "Recipient phone number cannot be null") @Pattern(
                    regexp = "^\\+[1-9]\\d{1,14}$",
                    message = "Phone number must be in E.164 format (e.g., +254712345678)")
                String>
            recipients)
    implements Serializable {

  public SmsRequest {
    recipients = List.copyOf(recipients);
  }
}
