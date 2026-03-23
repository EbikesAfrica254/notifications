package com.ebikes.notifications.listeners;

import org.springframework.stereotype.Component;

import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.services.events.InboxService;
import com.ebikes.notifications.services.notifications.MessageProcessor;
import com.ebikes.notifications.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequestListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final MessageProcessor messageProcessor;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(byte[] payload) {
    NotificationRequest event = objectMapper.readValue(payload, NotificationRequest.class);
    log.info("Received notification request: serviceReference={}", event.serviceReference());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    try {
      messageProcessor.process(event);
      inboxService.markProcessed(event.serviceReference());
    } catch (Exception e) {
      log.error("Failed to process notification: serviceReference={}", event.serviceReference(), e);
    }
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.startsWith("notifications.");
  }
}
