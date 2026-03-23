package com.ebikes.notifications.services.channels.whatsapp;

import java.io.Serializable;
import java.util.Map;

import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;

public interface WhatsAppChannelService {

  ChannelResponse send(String recipient, Map<String, Serializable> contextData);
}
