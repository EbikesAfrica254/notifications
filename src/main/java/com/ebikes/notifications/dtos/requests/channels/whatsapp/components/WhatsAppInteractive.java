package com.ebikes.notifications.dtos.requests.channels.whatsapp.components;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WhatsAppInteractive(
    String type, Header header, Body body, Footer footer, WhatsAppAction action)
    implements Serializable {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Header(String type, String text) implements Serializable {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Body(String text) implements Serializable {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Footer(String text) implements Serializable {}
}
