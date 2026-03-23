package com.ebikes.notifications.adapters.sms;

import com.ebikes.notifications.adapters.ChannelAdapter;
import com.ebikes.notifications.dtos.requests.channels.sms.SmsRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;

public interface SmsAdapter extends ChannelAdapter {
  ChannelResponse send(SmsRequest request);
}
