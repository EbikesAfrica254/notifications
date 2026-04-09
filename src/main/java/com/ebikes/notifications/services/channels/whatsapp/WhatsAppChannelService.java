package com.ebikes.notifications.services.channels.whatsapp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ebikes.notifications.adapters.channels.whatsapp.MetaWhatsAppAdapter;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppNotificationContext;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.mappers.WhatsAppMapper;
import com.ebikes.notifications.services.channels.ChannelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@ConditionalOnProperty(
    prefix = "notification.channels.whatsapp",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
@Service
@Slf4j
public class WhatsAppChannelService implements ChannelService {

  private final MetaWhatsAppAdapter provider;
  private final WhatsAppMapper mapper;
  private final ObjectMapper objectMapper;

  @Override
  public ChannelType getChannelType() {
    return ChannelType.WHATSAPP;
  }

  @Override
  public ChannelResponse send(Notification notification) {
    log.debug("Routing WhatsApp request to provider - recipient={}", notification.getRecipient());

    return provider.send(buildRequest(notification));
  }

  private WhatsAppRequest buildRequest(Notification notification) {
    try {
      WhatsAppNotificationContext context =
          objectMapper.readValue(notification.getMessageBody(), WhatsAppNotificationContext.class);
      return mapper.toRequest(context, notification.getRecipient());
    } catch (Exception e) {
      log.error(
          "Failed to deserialize WhatsApp message body - recipient={}",
          notification.getRecipient(),
          e);
      throw new InvalidStateException(
          ResponseCode.TEMPLATE_PROCESSING_ERROR,
          "Failed to deserialize WhatsApp message body: " + e.getMessage(),
          e);
    }
  }
}
