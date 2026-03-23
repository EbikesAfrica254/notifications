package com.ebikes.notifications.dtos.requests.channels.whatsapp.components;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WhatsAppButton(
    @NotBlank @Size(max = 20) String id, @NotBlank @Size(max = 20) String title)
    implements Serializable {}
