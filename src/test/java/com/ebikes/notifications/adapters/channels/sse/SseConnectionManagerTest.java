package com.ebikes.notifications.adapters.channels.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ebikes.notifications.configurations.properties.NotificationProperties;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Channels;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Sse;
import com.ebikes.notifications.exceptions.InvalidStateException;

@DisplayName("SseConnectionManager")
class SseConnectionManagerTest {

  private SseConnectionManager manager;

  private static final String RECIPIENT = "user-123";

  // Real properties instance — no mock needed, no stubbing mismatches possible
  private static NotificationProperties buildProperties(boolean enabled, int limit) {
    Sse sse = new Sse();
    sse.setEnabled(enabled);
    sse.setConnectionLimit(limit);
    sse.setConnectionTimeout(30_000L);

    Channels channels = new Channels();
    channels.setSse(sse);

    NotificationProperties props = new NotificationProperties();
    props.setChannels(channels);
    return props;
  }

  @BeforeEach
  void setUp() {
    manager = new SseConnectionManager(buildProperties(true, 10));
  }

  @Nested
  @DisplayName("createConnection")
  class CreateConnection {

    @Test
    @DisplayName("should throw InvalidStateException when SSE channel is disabled")
    void shouldThrowWhenDisabled() {
      SseConnectionManager disabledManager = new SseConnectionManager(buildProperties(false, 10));

      assertThatThrownBy(() -> disabledManager.createConnection(RECIPIENT))
          .isInstanceOf(InvalidStateException.class);
    }

    @Test
    @DisplayName("should throw InvalidStateException when connection limit is reached")
    void shouldThrowWhenConnectionLimitReached() {
      SseConnectionManager limitedManager = new SseConnectionManager(buildProperties(true, 1));

      limitedManager.createConnection(RECIPIENT);

      assertThatThrownBy(() -> limitedManager.createConnection("user-456"))
          .isInstanceOf(InvalidStateException.class);
    }

    @Test
    @DisplayName("should return a non-null SseEmitter on success")
    void shouldReturnEmitter() {
      assertThat(manager.createConnection(RECIPIENT)).isNotNull();
    }

    @Test
    @DisplayName("should register the connection so hasConnection returns true")
    void shouldRegisterConnection() {
      manager.createConnection(RECIPIENT);

      assertThat(manager.hasConnection(RECIPIENT)).isTrue();
    }

    @Test
    @DisplayName("should replace an existing connection without throwing")
    void shouldReplaceExistingConnection() {
      SseEmitter first = manager.createConnection(RECIPIENT);
      SseEmitter second = manager.createConnection(RECIPIENT);

      assertThat(second).isNotSameAs(first);
      assertThat(manager.hasConnection(RECIPIENT)).isTrue();
    }
  }

  @Nested
  @DisplayName("hasConnection")
  class HasConnection {

    @Test
    @DisplayName("should return true when connection exists")
    void shouldReturnTrueWhenPresent() {
      manager.createConnection(RECIPIENT);

      assertThat(manager.hasConnection(RECIPIENT)).isTrue();
    }

    @Test
    @DisplayName("should return false when no connection exists")
    void shouldReturnFalseWhenAbsent() {
      assertThat(manager.hasConnection(RECIPIENT)).isFalse();
    }
  }

  @Nested
  @DisplayName("removeConnection")
  class RemoveConnection {

    @Test
    @DisplayName("should remove an existing connection")
    void shouldRemoveExistingConnection() {
      manager.createConnection(RECIPIENT);
      manager.removeConnection(RECIPIENT);

      assertThat(manager.hasConnection(RECIPIENT)).isFalse();
    }

    @Test
    @DisplayName("should be idempotent when connection is absent")
    void shouldBeIdempotentWhenAbsent() {
      manager.removeConnection(RECIPIENT);

      assertThat(manager.hasConnection(RECIPIENT)).isFalse();
    }
  }

  @Nested
  @DisplayName("sendToUser")
  class SendToUser {

    @Test
    @DisplayName("should return false when recipient has no active connection")
    void shouldReturnFalseWhenNoConnection() {
      boolean result = manager.sendToUser(RECIPIENT, SseEmitter.event().data("test"));

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return true when event is sent successfully")
    void shouldReturnTrueOnSuccess() {
      manager.createConnection(RECIPIENT);

      boolean result = manager.sendToUser(RECIPIENT, SseEmitter.event().data("payload"));

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName(
        "should return false and remove connection when recipient has been explicitly removed")
    void shouldReturnFalseAfterExplicitRemoval() {
      manager.createConnection(RECIPIENT);
      manager.removeConnection(RECIPIENT);

      boolean result = manager.sendToUser(RECIPIENT, SseEmitter.event().data("payload"));

      assertThat(result).isFalse();
      assertThat(manager.hasConnection(RECIPIENT)).isFalse();
    }
  }

  @Nested
  @DisplayName("sendHeartbeat")
  class SendHeartbeat {

    @Test
    @DisplayName("should do nothing and not throw when no connections are active")
    void shouldSkipWhenEmpty() {
      manager.sendHeartbeat();

      assertThat(manager.hasConnection(RECIPIENT)).isFalse();
    }

    @Test
    @DisplayName("should ping all active connections without removing them")
    void shouldPingAllActiveConnections() {
      manager.createConnection(RECIPIENT);
      manager.createConnection("user-456");

      manager.sendHeartbeat();

      assertThat(manager.hasConnection(RECIPIENT)).isTrue();
      assertThat(manager.hasConnection("user-456")).isTrue();
    }

    @Test
    @DisplayName(
        "should not throw and leave no connections when map is empty after explicit removal")
    void shouldHandleAlreadyCompletedConnection() {
      manager.createConnection(RECIPIENT);
      manager.removeConnection(RECIPIENT);

      manager.sendHeartbeat();

      assertThat(manager.hasConnection(RECIPIENT)).isFalse();
    }
  }
}
