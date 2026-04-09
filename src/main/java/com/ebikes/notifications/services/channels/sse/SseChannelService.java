package com.ebikes.notifications.services.channels.sse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ebikes.notifications.adapters.channels.sse.SseConnectionManager;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.exceptions.RecipientOfflineException;
import com.ebikes.notifications.services.channels.ChannelService;
import com.ebikes.notifications.support.references.ReferenceGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class SseChannelService implements ChannelService {

  private static final String COST_CURRENCY = "KES";
  private static final String ENDPOINT = "sse://send-event";

  private final SseConnectionManager connectionManager;

  @Override
  public ChannelType getChannelType() {
    return ChannelType.SSE;
  }

  @Override
  public ChannelResponse send(Notification notification) {
    String recipient = notification.getRecipient();

    if (!connectionManager.hasConnection(recipient)) {
      log.debug(
          "No active SSE connection - recipient={} notification will remain PENDING", recipient);
      throw new RecipientOfflineException(notification.getId(), recipient);
    }

    String messageId = ReferenceGenerator.generateMessageReference(ChannelType.SSE);

    SseEmitter.SseEventBuilder event =
        SseEmitter.event().data(notification.getMessageBody()).id(messageId).name("notification");

    boolean sent = connectionManager.sendToUser(recipient, event);

    if (!sent) {
      log.debug(
          "SSE connection dropped during send - recipient={} notification will remain PENDING",
          recipient);
      throw new RecipientOfflineException(notification.getId(), recipient);
    }

    log.info("SSE message sent - recipient={} messageId={}", recipient, messageId);

    return new ChannelResponse(
        BigDecimal.ZERO,
        COST_CURRENCY,
        Map.of("recipient", recipient),
        messageId,
        ENDPOINT,
        OffsetDateTime.now());
  }
}
