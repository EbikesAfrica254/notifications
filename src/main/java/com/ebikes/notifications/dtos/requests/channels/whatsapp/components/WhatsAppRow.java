package com.ebikes.notifications.dtos.requests.channels.whatsapp.components;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WhatsAppRow(
    @Size(max = 72) String description,
    @NotBlank @Size(max = 200) String id,
    @NotBlank @Size(max = 24) String title)
    implements Serializable {}
