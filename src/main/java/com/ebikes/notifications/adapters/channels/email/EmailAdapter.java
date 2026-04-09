package com.ebikes.notifications.adapters.channels.email;

import com.ebikes.notifications.adapters.channels.ChannelAdapter;
import com.ebikes.notifications.dtos.requests.channels.email.EmailRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;

public interface EmailAdapter extends ChannelAdapter {
  ChannelResponse send(EmailRequest request);
}
