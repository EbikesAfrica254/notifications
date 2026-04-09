package com.ebikes.notifications.services.channels.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.notifications.adapters.channels.whatsapp.MetaWhatsAppAdapter;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppNotificationContext;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.mappers.WhatsAppMapper;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;

import tools.jackson.databind.ObjectMapper;

@DisplayName("WhatsAppChannelService")
@ExtendWith(MockitoExtension.class)
class WhatsAppChannelServiceTest {

  private static final String TEST_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000002";
  private static final String TEST_RECIPIENT = "+254700000001";

  @Mock private MetaWhatsAppAdapter provider;
  @Mock private WhatsAppMapper mapper;
  @Mock private ObjectMapper objectMapper;

  private WhatsAppChannelService service;

  @BeforeEach
  void setUp() {
    service = new WhatsAppChannelService(provider, mapper, objectMapper);
  }

  @Nested
  @DisplayName("getChannelType")
  class GetChannelType {

    @Test
    @DisplayName("should return WHATSAPP")
    void shouldReturnWhatsApp() {
      assertThat(service.getChannelType()).isEqualTo(ChannelType.WHATSAPP);
    }
  }

  @Nested
  @DisplayName("send")
  class Send {

    @Test
    @DisplayName(
        "should deserialize message body, map to request, delegate to provider and return response")
    void shouldDelegateToProviderAndReturnResponse() {
      Notification notification =
          NotificationFixtures.forOrganizationAndBranch(
              TEST_ORGANIZATION_ID, null, TEST_RECIPIENT, ChannelType.WHATSAPP);
      WhatsAppNotificationContext context =
          new WhatsAppNotificationContext(
              "Hello", null, null, null, null, null, null, "TEXT", null, null, null);
      WhatsAppRequest request =
          new WhatsAppRequest(
              "Hello",
              null,
              null,
              null,
              null,
              null,
              null,
              com.ebikes.notifications.enums.WhatsAppMessageType.TEXT,
              null,
              null,
              TEST_RECIPIENT,
              null);
      ChannelResponse channelResponse =
          new ChannelResponse(
              BigDecimal.ZERO,
              "KES",
              Map.of("type", "TEXT"),
              "msg-123",
              "meta://send-whatsapp-message",
              OffsetDateTime.now());

      when(objectMapper.readValue(notification.getMessageBody(), WhatsAppNotificationContext.class))
          .thenReturn(context);
      when(mapper.toRequest(context, TEST_RECIPIENT)).thenReturn(request);
      when(provider.send(request)).thenReturn(channelResponse);

      ChannelResponse result = service.send(notification);

      assertThat(result).isEqualTo(channelResponse);
      verify(provider).send(request);
    }

    @Test
    @DisplayName("should throw InvalidStateException when message body cannot be deserialized")
    void shouldThrowInvalidStateExceptionOnDeserializationFailure() {
      Notification notification =
          NotificationFixtures.forOrganizationAndBranch(
              TEST_ORGANIZATION_ID, null, TEST_RECIPIENT, ChannelType.WHATSAPP);
      when(objectMapper.readValue(notification.getMessageBody(), WhatsAppNotificationContext.class))
          .thenThrow(new RuntimeException("bad json"));

      assertThatThrownBy(() -> service.send(notification))
          .isInstanceOf(InvalidStateException.class);
    }
  }
}
