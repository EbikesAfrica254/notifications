package com.ebikes.notifications.adapters.channels.sms;

import com.ebikes.notifications.adapters.channels.ChannelAdapter;
import com.ebikes.notifications.dtos.requests.channels.sms.SmsRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;

public interface SmsAdapter extends ChannelAdapter {
  ChannelResponse send(SmsRequest request);
}
