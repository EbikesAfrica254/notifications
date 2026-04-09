package com.ebikes.notifications.services.channels.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.notifications.adapters.channels.sms.TaifaMobileSmsAdapter;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.requests.channels.sms.SmsRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;

@DisplayName("SmsChannelService")
@ExtendWith(MockitoExtension.class)
class SmsChannelServiceTest {

  private static final String TEST_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000002";
  private static final String TEST_PHONE_NUMBER = "+254700000001";

  @Mock private TaifaMobileSmsAdapter smsProvider;

  private SmsChannelService service;

  @BeforeEach
  void setUp() {
    service = new SmsChannelService(smsProvider);
  }

  @Nested
  @DisplayName("getChannelType")
  class GetChannelType {

    @Test
    @DisplayName("should return SMS")
    void shouldReturnSms() {
      assertThat(service.getChannelType()).isEqualTo(ChannelType.SMS);
    }
  }

  @Nested
  @DisplayName("send")
  class Send {

    @Test
    @DisplayName("should delegate to adapter with correct request and return response")
    void shouldDelegateToAdapterAndReturnResponse() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_PHONE_NUMBER);
      ChannelResponse channelResponse =
          new ChannelResponse(
              new BigDecimal("0.50"),
              "KES",
              Map.of("status", "dispatched"),
              "msg-123",
              "taifa://send-sms",
              OffsetDateTime.now());
      when(smsProvider.send(any(SmsRequest.class))).thenReturn(channelResponse);

      ChannelResponse result = service.send(notification);

      assertThat(result).isEqualTo(channelResponse);
      verify(smsProvider)
          .send(
              new SmsRequest(notification.getMessageBody(), List.of(notification.getRecipient())));
    }

    @Test
    @DisplayName("should propagate ExternalServiceException thrown by adapter")
    void shouldPropagateExternalServiceException() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_PHONE_NUMBER);
      when(smsProvider.send(any(SmsRequest.class))).thenThrow(ExternalServiceException.class);

      assertThatThrownBy(() -> service.send(notification))
          .isInstanceOf(ExternalServiceException.class);
    }
  }
}
