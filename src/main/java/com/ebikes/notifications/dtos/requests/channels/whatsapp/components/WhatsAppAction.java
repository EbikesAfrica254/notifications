package com.ebikes.notifications.dtos.requests.channels.whatsapp.components;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WhatsAppAction(
    List<Button> buttons, String button, List<Section> sections, List<UrlButton> urlButtons)
    implements Serializable {

  public WhatsAppAction {
    buttons = buttons != null ? List.copyOf(buttons) : List.of();
    sections = sections != null ? List.copyOf(sections) : List.of();
    urlButtons = urlButtons != null ? List.copyOf(urlButtons) : List.of();
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Button(String type, String id, String title) implements Serializable {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UrlButton(String type, String url, @JsonProperty("display_text") String displayText)
      implements Serializable {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Section(String title, List<Row> rows) implements Serializable {
    public Section {
      rows = rows != null ? List.copyOf(rows) : List.of();
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Row(String id, String title, String description) implements Serializable {}
}
