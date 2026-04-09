package com.ebikes.notifications.listeners;

import static com.ebikes.notifications.constants.EventConstants.ExternalContracts;
import static com.ebikes.notifications.constants.EventConstants.ExternalContracts.USER_CONFIGURATION;
import static com.ebikes.notifications.constants.EventConstants.Source.HOST_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ebikes.notifications.dtos.events.incoming.UserConfigurationRequest;
import com.ebikes.notifications.services.events.InboxService;
import com.ebikes.notifications.services.preferences.UserPreferenceService;
import com.ebikes.notifications.support.context.EventContext;
import com.ebikes.notifications.support.fixtures.SecurityFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractListenerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("UserConfigurationRequestListener")
class UserConfigurationRequestListenerTest extends AbstractListenerTest {

  private static final String EVENT_TYPE = USER_CONFIGURATION;
  private static final String SERVICE_REF = "test-service-reference";
  private static final String SOURCE = HOST_SERVICE;

  @Mock private InboxService inboxService;
  @Mock private ObjectMapper objectMapper;
  @Mock private UserPreferenceService userPreferenceService;

  @InjectMocks private UserConfigurationRequestListener listener;

  private final byte[] payload = new byte[0];
  private UserConfigurationRequest event;

  @BeforeEach
  void setUp() {
    event =
        new UserConfigurationRequest(
            EVENT_TYPE,
            SecurityFixtures.TEST_USER_ID,
            SecurityFixtures.TEST_ORGANIZATION_ID,
            SERVICE_REF,
            Instant.now());

    EventContext.set(
        "test-correlation-id", EVENT_TYPE, ExternalContracts.USER_CONFIGURATION, SOURCE);
  }

  @Nested
  @DisplayName("matches")
  class Matches {

    @Test
    @DisplayName("returns true for user configuration routing key")
    void returnsTrueForUserConfigurationKey() {
      assertThat(listener.matches(USER_CONFIGURATION)).isTrue();
    }

    @Test
    @DisplayName("returns false for other routing keys")
    void returnsFalseForOtherKeys() {
      assertThat(listener.matches(ExternalContracts.ORGANIZATION_CONFIGURATION)).isFalse();
      assertThat(listener.matches("notifications.email")).isFalse();
    }
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @Test
    @DisplayName("creates defaults and marks inbox processed when event is accepted")
    void createsDefaultsAndMarksInboxWhenAccepted() throws Exception {
      given(objectMapper.readValue(payload, UserConfigurationRequest.class)).willReturn(event);
      given(inboxService.receive(EVENT_TYPE, SERVICE_REF, SOURCE)).willReturn(true);

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(userPreferenceService)
          .should()
          .createDefaults(SecurityFixtures.TEST_USER_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      then(inboxService).should().markProcessed(SERVICE_REF);
    }

    @Test
    @DisplayName("skips processing when inbox rejects duplicate")
    void skipsWhenInboxRejectsDuplicate() throws Exception {
      given(objectMapper.readValue(payload, UserConfigurationRequest.class)).willReturn(event);
      given(inboxService.receive(EVENT_TYPE, SERVICE_REF, SOURCE)).willReturn(false);

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(userPreferenceService).shouldHaveNoInteractions();
      then(inboxService).should(never()).markProcessed(any());
    }

    @Test
    @DisplayName("skips all processing when event context is absent")
    void skipsWhenEventContextAbsent() {
      given(objectMapper.readValue(payload, UserConfigurationRequest.class)).willReturn(event);
      EventContext.clear();

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(inboxService).shouldHaveNoInteractions();
      then(userPreferenceService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("swallows exception thrown by preference service")
    void swallowsExceptionFromPreferenceService() throws Exception {
      given(objectMapper.readValue(payload, UserConfigurationRequest.class)).willReturn(event);
      given(inboxService.receive(EVENT_TYPE, SERVICE_REF, SOURCE)).willReturn(true);
      willThrow(new RuntimeException("db error"))
          .given(userPreferenceService)
          .createDefaults(SecurityFixtures.TEST_USER_ID, SecurityFixtures.TEST_ORGANIZATION_ID);

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(inboxService).should(never()).markProcessed(any());
    }
  }
}
