package com.ebikes.notifications.services.channels.whatsapp;

import java.io.Serializable;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ebikes.notifications.adapters.whatsapp.MetaWhatsAppAdapter;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppNotificationContext;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.mappers.WhatsAppMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@ConditionalOnProperty(
    prefix = "notification.channels.whatsapp",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppChannelServiceImpl implements WhatsAppChannelService {

  private final MetaWhatsAppAdapter provider;
  private final WhatsAppMapper mapper;
  private final ObjectMapper objectMapper;

  @Override
  public ChannelResponse send(String recipient, Map<String, Serializable> contextData) {
    log.debug("Building WhatsApp request for recipient: {}", recipient);

    WhatsAppRequest request = buildRequest(recipient, contextData);

    log.debug("Sending WhatsApp message to: {} with type: {}", request.to(), request.messageType());

    ChannelResponse response = provider.send(request);

    log.debug(
        "WhatsApp message sent successfully. MessageId: {}, Recipient: {}",
        response.providerMessageId(),
        recipient);

    return response;
  }

  private WhatsAppRequest buildRequest(String recipient, Map<String, Serializable> contextData) {
    if (contextData == null || !contextData.containsKey("whatsapp")) {
      throw new IllegalArgumentException("WhatsApp configuration missing in contextData");
    }

    Object whatsappData = contextData.get("whatsapp");

    WhatsAppNotificationContext context =
        objectMapper.convertValue(whatsappData, WhatsAppNotificationContext.class);

    return mapper.toRequest(context, recipient);
  }
}
