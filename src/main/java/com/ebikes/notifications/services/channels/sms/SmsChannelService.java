package com.ebikes.notifications.services.channels.sms;

import com.ebikes.notifications.dtos.requests.channels.sms.SmsRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.RateLimitException;

public interface SmsChannelService {

  ChannelResponse send(SmsRequest request) throws ExternalServiceException, RateLimitException;
}
