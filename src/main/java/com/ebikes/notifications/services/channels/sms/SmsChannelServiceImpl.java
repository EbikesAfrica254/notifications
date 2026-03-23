package com.ebikes.notifications.services.channels.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ebikes.notifications.adapters.sms.TaifaMobileSmsAdapter;
import com.ebikes.notifications.dtos.requests.channels.sms.SmsRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.RateLimitException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(
    prefix = "notification.channels.sms",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
@Service
@Slf4j
public class SmsChannelServiceImpl implements SmsChannelService {

  private final TaifaMobileSmsAdapter smsProvider;

  @Override
  public ChannelResponse send(SmsRequest request)
      throws ExternalServiceException, RateLimitException {
    log.debug("Routing SMS request to provider - recipients={}", request.recipients());
    return smsProvider.send(request);
  }
}
