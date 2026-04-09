package com.ebikes.notifications.dtos.requests.channels.whatsapp.components;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WhatsAppUrlButton(@NotBlank String url, @NotBlank @Size(max = 20) String displayText)
    implements Serializable {}
