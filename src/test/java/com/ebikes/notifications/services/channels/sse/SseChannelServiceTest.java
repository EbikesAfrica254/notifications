package com.ebikes.notifications.services.channels.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ebikes.notifications.adapters.channels.sse.SseConnectionManager;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.exceptions.RecipientOfflineException;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;

@DisplayName("SseChannelService")
@ExtendWith(MockitoExtension.class)
class SseChannelServiceTest {

  private static final String TEST_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000002";
  private static final String TEST_RECIPIENT = "00000000-0000-0000-0000-000000000001";

  @Mock private SseConnectionManager connectionManager;

  private SseChannelService service;

  @BeforeEach
  void setUp() {
    service = new SseChannelService(connectionManager);
  }

  @Nested
  @DisplayName("getChannelType")
  class GetChannelType {

    @Test
    @DisplayName("should return SSE")
    void shouldReturnSse() {
      assertThat(service.getChannelType()).isEqualTo(ChannelType.SSE);
    }
  }

  @Nested
  @DisplayName("send")
  class Send {

    @Test
    @DisplayName("should throw RecipientOfflineException when recipient has no active connection")
    void shouldThrowWhenNoConnection() {
      Notification notification =
          NotificationFixtures.forOrganizationAndBranch(
              TEST_ORGANIZATION_ID, null, TEST_RECIPIENT, ChannelType.SSE);
      when(connectionManager.hasConnection(TEST_RECIPIENT)).thenReturn(false);

      assertThatThrownBy(() -> service.send(notification))
          .isInstanceOf(RecipientOfflineException.class);
      verify(connectionManager, never()).sendToUser(any(), any());
    }

    @Test
    @DisplayName("should throw RecipientOfflineException when connection drops during send")
    void shouldThrowWhenConnectionDropsDuringSend() {
      Notification notification =
          NotificationFixtures.forOrganizationAndBranch(
              TEST_ORGANIZATION_ID, null, TEST_RECIPIENT, ChannelType.SSE);
      when(connectionManager.hasConnection(TEST_RECIPIENT)).thenReturn(true);
      when(connectionManager.sendToUser(eq(TEST_RECIPIENT), any(SseEmitter.SseEventBuilder.class)))
          .thenReturn(false);

      assertThatThrownBy(() -> service.send(notification))
          .isInstanceOf(RecipientOfflineException.class);
    }

    @Test
    @DisplayName("should return channel response when event is sent successfully")
    void shouldReturnChannelResponseOnSuccess() {
      Notification notification =
          NotificationFixtures.forOrganizationAndBranch(
              TEST_ORGANIZATION_ID, null, TEST_RECIPIENT, ChannelType.SSE);
      when(connectionManager.hasConnection(TEST_RECIPIENT)).thenReturn(true);
      when(connectionManager.sendToUser(eq(TEST_RECIPIENT), any(SseEmitter.SseEventBuilder.class)))
          .thenReturn(true);

      ChannelResponse result = service.send(notification);

      assertThat(result.providerName()).isEqualTo("sse://send-event");
      assertThat(result.costCurrency()).isEqualTo("KES");
    }
  }
}
