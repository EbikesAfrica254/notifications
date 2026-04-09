package com.ebikes.notifications.adapters.channels.whatsapp;

import com.ebikes.notifications.adapters.channels.ChannelAdapter;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;

public interface WhatsAppAdapter extends ChannelAdapter {
  ChannelResponse send(WhatsAppRequest request);
}
