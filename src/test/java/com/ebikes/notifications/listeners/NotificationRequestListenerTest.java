package com.ebikes.notifications.listeners;

import static com.ebikes.notifications.constants.EventConstants.ExternalContracts.ORGANIZATION_CONFIGURATION;
import static com.ebikes.notifications.constants.EventConstants.Source.HOST_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
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

import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.services.events.InboxService;
import com.ebikes.notifications.services.notifications.MessageProcessor;
import com.ebikes.notifications.support.context.EventContext;
import com.ebikes.notifications.support.fixtures.NotificationRequestFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractListenerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("NotificationRequestListener")
class NotificationRequestListenerTest extends AbstractListenerTest {

  private static final String EVENT_TYPE = "notifications.email";
  private static final String SOURCE = HOST_SERVICE;

  @Mock private InboxService inboxService;
  @Mock private MessageProcessor messageProcessor;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private NotificationRequestListener listener;

  private final byte[] payload = new byte[0];
  private NotificationRequest event;

  @BeforeEach
  void setUp() {
    event = NotificationRequestFixtures.accountVerification();
    EventContext.set("test-correlation-id", EVENT_TYPE, "notifications.email", SOURCE);
  }

  @Nested
  @DisplayName("matches")
  class Matches {

    @Test
    @DisplayName("returns true for notifications. prefix")
    void returnsTrueForNotificationsPrefix() {
      assertThat(listener.matches("notifications.email")).isTrue();
      assertThat(listener.matches("notifications.sms")).isTrue();
    }

    @Test
    @DisplayName("returns false for other routing keys")
    void returnsFalseForOtherKeys() {
      assertThat(listener.matches(ORGANIZATION_CONFIGURATION)).isFalse();
      assertThat(listener.matches("audit.something")).isFalse();
    }
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @BeforeEach
    void setUp() {
      given(objectMapper.readValue(payload, NotificationRequest.class)).willReturn(event);
    }

    @Test
    @DisplayName("processes event and marks inbox when inbox accepts")
    void processesAndMarksInboxWhenAccepted() {
      given(inboxService.receive(EVENT_TYPE, event.serviceReference(), SOURCE)).willReturn(true);

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(messageProcessor).should().process(event);
      then(inboxService).should().markProcessed(event.serviceReference());
    }

    @Test
    @DisplayName("skips processing when inbox rejects duplicate")
    void skipsWhenInboxRejectsDuplicate() {
      given(inboxService.receive(EVENT_TYPE, event.serviceReference(), SOURCE)).willReturn(false);

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(messageProcessor).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("skips all processing when event context is absent")
    void skipsWhenEventContextAbsent() {
      EventContext.clear();

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(inboxService).shouldHaveNoInteractions();
      then(messageProcessor).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("swallows exception from message processor without propagating")
    void swallowsExceptionFromProcessor() {
      given(inboxService.receive(EVENT_TYPE, event.serviceReference(), SOURCE)).willReturn(true);
      willThrow(new RuntimeException("channel failure")).given(messageProcessor).process(event);

      assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();

      then(inboxService).should(never()).markProcessed(any());
    }
  }
}
