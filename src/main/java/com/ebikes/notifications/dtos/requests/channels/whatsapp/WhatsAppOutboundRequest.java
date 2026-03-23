package com.ebikes.notifications.dtos.requests.channels.whatsapp;

import java.io.Serializable;

import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppDocument;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppInteractive;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppText;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WhatsAppOutboundRequest(
    @JsonProperty("messaging_product") String messagingProduct,
    String to,
    String type,
    WhatsAppText text,
    WhatsAppInteractive interactive,
    WhatsAppDocument document)
    implements Serializable {}
