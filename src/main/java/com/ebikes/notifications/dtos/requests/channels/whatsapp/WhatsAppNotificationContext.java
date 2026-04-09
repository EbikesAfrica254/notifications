package com.ebikes.notifications.dtos.requests.channels.whatsapp;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WhatsAppNotificationContext(
    String body,
    @JsonProperty("buttonText") String buttonText,
    List<ButtonContext> buttons,
    String caption,
    @JsonProperty("documentUrl") String documentUrl,
    String filename,
    String footer,
    @JsonProperty("messageType") String messageType,
    @JsonProperty("previewUrl") Boolean previewUrl,
    List<SectionContext> sections,
    List<UrlButtonContext> urlButtons)
    implements Serializable {

  public WhatsAppNotificationContext {
    buttons = buttons != null ? List.copyOf(buttons) : List.of();
    sections = sections != null ? List.copyOf(sections) : List.of();
    urlButtons = urlButtons != null ? List.copyOf(urlButtons) : List.of();
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ButtonContext(String id, String title) implements Serializable {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UrlButtonContext(String url, @JsonProperty("display_text") String displayText)
      implements Serializable {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record SectionContext(String title, List<RowContext> rows) implements Serializable {
    public SectionContext {
      rows = rows != null ? List.copyOf(rows) : List.of();
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record RowContext(String id, String title, String description) implements Serializable {}
}
