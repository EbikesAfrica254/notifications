package com.ebikes.notifications.dtos.requests.channels.whatsapp.components;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record WhatsAppSection(
    @Valid @NotEmpty @Size(max = 10) List<WhatsAppRow> rows, @NotBlank @Size(max = 24) String title)
    implements Serializable {
  public WhatsAppSection {
    rows = rows != null ? List.copyOf(rows) : List.of();
  }
}
