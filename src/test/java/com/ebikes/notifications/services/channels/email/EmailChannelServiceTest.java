package com.ebikes.notifications.services.channels.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.ebikes.notifications.adapters.channels.email.SesEmailAdapter;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.requests.channels.email.EmailRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;

@DisplayName("EmailChannelService")
@ExtendWith(MockitoExtension.class)
class EmailChannelServiceTest {

  private static final String TEST_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000002";
  private static final String TEST_EMAIL = "test@ebikes.test";

  @Mock private SesEmailAdapter emailProvider;

  private EmailChannelService service;

  @BeforeEach
  void setUp() {
    service = new EmailChannelService(emailProvider);
  }

  @Nested
  @DisplayName("getChannelType")
  class GetChannelType {

    @Test
    @DisplayName("should return EMAIL")
    void shouldReturnEmail() {
      assertThat(service.getChannelType()).isEqualTo(ChannelType.EMAIL);
    }
  }

  @Nested
  @DisplayName("send")
  class Send {

    @Test
    @DisplayName("should delegate to adapter with correct request and return response")
    void shouldDelegateToAdapterAndReturnResponse() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      ChannelResponse channelResponse =
          new ChannelResponse(
              new BigDecimal("0.0001"),
              "USD",
              Map.of("provider", "aws-ses"),
              "msg-123",
              "ses://send-email",
              OffsetDateTime.now());
      when(emailProvider.send(any(EmailRequest.class))).thenReturn(channelResponse);

      ChannelResponse result = service.send(notification);

      assertThat(result).isEqualTo(channelResponse);
      verify(emailProvider)
          .send(
              new EmailRequest(
                  notification.getMessageBody(),
                  notification.getRecipient(),
                  notification.getMessageSubject()));
    }

    @Test
    @DisplayName("should propagate ExternalServiceException thrown by adapter")
    void shouldPropagateExternalServiceException() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      when(emailProvider.send(any(EmailRequest.class))).thenThrow(ExternalServiceException.class);

      assertThatThrownBy(() -> service.send(notification))
          .isInstanceOf(ExternalServiceException.class);
    }
  }
}
