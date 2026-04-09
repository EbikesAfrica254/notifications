package com.ebikes.notifications.services.channels.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ebikes.notifications.adapters.channels.email.SesEmailAdapter;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.requests.channels.email.EmailRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.services.channels.ChannelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(
    prefix = "notification.channels.email",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
@Service
@Slf4j
public class EmailChannelService implements ChannelService {

  private final SesEmailAdapter emailProvider;

  @Override
  public ChannelType getChannelType() {
    return ChannelType.EMAIL;
  }

  @Override
  public ChannelResponse send(Notification notification) {
    log.debug("Routing email request to provider - recipient={}", notification.getRecipient());

    return emailProvider.send(
        new EmailRequest(
            notification.getMessageBody(),
            notification.getRecipient(),
            notification.getMessageSubject()));
  }
}
