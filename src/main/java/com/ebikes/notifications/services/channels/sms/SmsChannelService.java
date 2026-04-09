package com.ebikes.notifications.services.channels.sms;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ebikes.notifications.adapters.channels.sms.TaifaMobileSmsAdapter;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.requests.channels.sms.SmsRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.services.channels.ChannelService;

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
public class SmsChannelService implements ChannelService {

  private final TaifaMobileSmsAdapter smsProvider;

  @Override
  public ChannelType getChannelType() {
    return ChannelType.SMS;
  }

  @Override
  public ChannelResponse send(Notification notification) {
    log.debug("Routing SMS request to provider - recipient={}", notification.getRecipient());

    return smsProvider.send(
        new SmsRequest(notification.getMessageBody(), List.of(notification.getRecipient())));
  }
}
