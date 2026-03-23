package com.ebikes.notifications.adapters.email;

import com.ebikes.notifications.adapters.ChannelAdapter;
import com.ebikes.notifications.dtos.requests.channels.email.EmailRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;

public interface EmailAdapter extends ChannelAdapter {
  ChannelResponse send(EmailRequest request);
}
