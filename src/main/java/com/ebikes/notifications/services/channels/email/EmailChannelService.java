package com.ebikes.notifications.services.channels.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ebikes.notifications.adapters.email.SesEmailAdapter;
import com.ebikes.notifications.dtos.requests.channels.email.EmailRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.exceptions.ExternalServiceException;

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
public class EmailChannelService {

  private final SesEmailAdapter emailProvider;

  public ChannelResponse send(EmailRequest request) throws ExternalServiceException {
    log.debug("Routing email request to provider - recipient={}", request.recipient());
    return emailProvider.send(request);
  }
}
