package com.ebikes.notifications.dtos.requests.channels.whatsapp;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppButton;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppSection;
import com.ebikes.notifications.enums.WhatsAppMessageType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WhatsAppRequest(
    String body,
    String buttonText,
    List<WhatsAppButton> buttons,
    String caption,
    String documentUrl,
    String filename,
    String footer,
    @NotNull WhatsAppMessageType messageType,
    Boolean previewUrl,
    List<WhatsAppSection> sections,
    @NotBlank String to)
    implements Serializable {
  public WhatsAppRequest {
    buttons = buttons != null ? List.copyOf(buttons) : List.of();
    sections = sections != null ? List.copyOf(sections) : List.of();
  }
}
