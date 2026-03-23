package com.ebikes.notifications.dtos.requests.channels.whatsapp.components;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WhatsAppDocument(String link, String filename, String caption)
    implements Serializable {}
