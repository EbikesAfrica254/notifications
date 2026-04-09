package com.ebikes.notifications.listeners;

import org.springframework.stereotype.Component;

import com.ebikes.notifications.constants.EventConstants.ExternalContracts;
import com.ebikes.notifications.dtos.events.incoming.UserConfigurationRequest;
import com.ebikes.notifications.services.events.InboxService;
import com.ebikes.notifications.services.preferences.UserPreferenceService;
import com.ebikes.notifications.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserConfigurationRequestListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final ObjectMapper objectMapper;
  private final UserPreferenceService userPreferenceService;

  @Override
  public void handle(byte[] payload) {
    UserConfigurationRequest event =
        objectMapper.readValue(payload, UserConfigurationRequest.class);
    log.info(
        "Received UserConfigurationRequest - keycloakUserId={} organizationId={}",
        event.keycloakUserId(),
        event.organizationId());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    try {
      userPreferenceService.createDefaults(event.keycloakUserId(), event.organizationId());
      inboxService.markProcessed(event.serviceReference());
    } catch (Exception e) {
      log.error(
          "Failed to process UserConfigurationRequest: serviceReference={}",
          event.serviceReference(),
          e);
    }
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.equals(ExternalContracts.USER_CONFIGURATION);
  }
}
