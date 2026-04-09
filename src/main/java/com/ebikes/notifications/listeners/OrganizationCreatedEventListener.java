package com.ebikes.notifications.listeners;

import org.springframework.stereotype.Component;

import com.ebikes.notifications.constants.EventConstants.ExternalContracts;
import com.ebikes.notifications.dtos.events.incoming.OrganizationCreatedEvent;
import com.ebikes.notifications.services.events.InboxService;
import com.ebikes.notifications.services.preferences.OrganizationPreferenceService;
import com.ebikes.notifications.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationCreatedEventListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final ObjectMapper objectMapper;
  private final OrganizationPreferenceService organizationPreferenceService;

  @Override
  public void handle(byte[] payload) {
    OrganizationCreatedEvent event =
        objectMapper.readValue(payload, OrganizationCreatedEvent.class);
    log.info("Received OrganizationCreatedEvent - organizationId={}", event.organizationId());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    try {
      organizationPreferenceService.createDefaults(event.organizationId());
      inboxService.markProcessed(event.serviceReference());
    } catch (Exception e) {
      log.error(
          "Failed to process OrganizationCreatedEvent: serviceReference={}",
          event.serviceReference(),
          e);
    }
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.equals(ExternalContracts.ORGANIZATION_CONFIGURATION);
  }
}
