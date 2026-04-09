package com.ebikes.notifications.listeners;

import static com.ebikes.notifications.constants.EventConstants.ExternalContracts.ORGANIZATION_CONFIGURATION;
import static com.ebikes.notifications.constants.EventConstants.Source.HOST_SERVICE;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ebikes.notifications.constants.EventConstants;
import com.ebikes.notifications.dtos.events.incoming.OrganizationCreatedEvent;
import com.ebikes.notifications.services.events.InboxService;
import com.ebikes.notifications.services.preferences.OrganizationPreferenceService;
import com.ebikes.notifications.support.context.EventContext;
import com.ebikes.notifications.support.infrastructure.AbstractListenerTest;

import tools.jackson.databind.ObjectMapper;

class OrganizationCreatedEventListenerTest extends AbstractListenerTest {

  private static final String SERVICE_REF = "test-service-reference";

  @Mock private InboxService inboxService;
  @Mock private ObjectMapper objectMapper;
  @Mock private OrganizationPreferenceService organizationPreferenceService;

  @InjectMocks private OrganizationCreatedEventListener listener;

  private final byte[] payload = new byte[0];

  @BeforeEach
  void setUp() {
    OrganizationCreatedEvent event =
        new OrganizationCreatedEvent(
            "Test Organization", TEST_ORGANIZATION_ID, ORGANIZATION_CONFIGURATION, SERVICE_REF);

    given(objectMapper.readValue(payload, OrganizationCreatedEvent.class)).willReturn(event);

    EventContext.set(
        "test-correlation-id",
        ORGANIZATION_CONFIGURATION,
        EventConstants.ExternalContracts.ORGANIZATION_CONFIGURATION,
        HOST_SERVICE);
  }

  @Nested
  class Handle {

    @Test
    @DisplayName("creates defaults and marks inbox processed when event is accepted")
    void createsDefaultsAndMarksInboxWhenAccepted() {
      given(inboxService.receive(ORGANIZATION_CONFIGURATION, SERVICE_REF, HOST_SERVICE))
          .willReturn(true);

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(organizationPreferenceService).should().createDefaults(TEST_ORGANIZATION_ID);
      then(inboxService).should().markProcessed(SERVICE_REF);
    }

    @Test
    @DisplayName("skips processing when event is a duplicate")
    void skipsWhenDuplicate() {
      given(inboxService.receive(ORGANIZATION_CONFIGURATION, SERVICE_REF, HOST_SERVICE))
          .willReturn(false);

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(organizationPreferenceService).should(never()).createDefaults(any());
      then(inboxService).should(never()).markProcessed(any());
    }

    @Test
    @DisplayName("swallows exception thrown by preference service")
    void swallowsExceptionFromPreferenceService() {
      given(inboxService.receive(ORGANIZATION_CONFIGURATION, SERVICE_REF, HOST_SERVICE))
          .willReturn(true);
      willThrow(new RuntimeException("db error"))
          .given(organizationPreferenceService)
          .createDefaults(TEST_ORGANIZATION_ID);

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(inboxService).should(never()).markProcessed(any());
    }

    @Test
    @DisplayName("skips all processing when event context is absent")
    void skipsWhenEventContextAbsent() {
      EventContext.clear();

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(inboxService).shouldHaveNoInteractions();
      then(organizationPreferenceService).shouldHaveNoInteractions();
    }
  }
}
