package com.ebikes.notifications.dtos.requests.channels.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailRequest(
    @NotBlank(message = "Email body is required") String body,
    @Email(message = "Invalid email address") @NotBlank(message = "Recipient email is required") String recipient,
    @NotBlank(message = "Email subject is required") @Size(max = 500, message = "Subject must not exceed 500 characters") String subject) {}
