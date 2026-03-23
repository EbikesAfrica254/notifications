package com.ebikes.notifications.dtos.requests.channels.whatsapp.components;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WhatsAppText(String body, @JsonProperty("preview_url") Boolean previewUrl)
    implements Serializable {}
