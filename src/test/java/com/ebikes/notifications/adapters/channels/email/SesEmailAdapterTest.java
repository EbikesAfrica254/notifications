package com.ebikes.notifications.adapters.channels.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.notifications.configurations.properties.NotificationProperties;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Channels;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Email;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Ses;
import com.ebikes.notifications.dtos.requests.channels.email.EmailRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.exceptions.ExternalServiceException;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

@DisplayName("SesEmailAdapter")
@ExtendWith(MockitoExtension.class)
class SesEmailAdapterTest {

  @Mock private NotificationProperties properties;
  @Mock private SesClient sesClient;

  private SesEmailAdapter adapter;

  private final EmailRequest request =
      new EmailRequest("<p>Hello</p>", "recipient@test.com", "Test Subject");

  @BeforeEach
  void setUp() {
    Ses ses = new Ses();
    ses.setSenderAddress("noreply@ebikes.test");
    ses.setRegion("us-east-1");

    Email email = new Email();
    email.setSes(ses);

    Channels channels = new Channels();
    channels.setEmail(email);

    when(properties.getChannels()).thenReturn(channels);

    adapter = new SesEmailAdapter(properties, sesClient);
  }

  @Nested
  @DisplayName("send")
  class Send {

    @Test
    @DisplayName("should return ChannelResponse with messageId on success")
    void shouldReturnChannelResponseOnSuccess() {
      SendEmailResponse sesResponse = SendEmailResponse.builder().messageId("msg-abc-123").build();
      when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(sesResponse);

      ChannelResponse result = adapter.send(request);

      assertThat(result.providerMessageId()).isEqualTo("msg-abc-123");
      assertThat(result.costAmount()).isEqualTo(new BigDecimal("0.0001"));
      assertThat(result.costCurrency()).isEqualTo("USD");
      assertThat(result.providerName()).isEqualTo("ses://send-email");
      assertThat(result.sentAt()).isNotNull();
    }

    @Test
    @DisplayName("should throw ExternalServiceException when SesException is thrown")
    void shouldThrowExternalServiceExceptionOnSesFailure() {
      AwsErrorDetails errorDetails =
          AwsErrorDetails.builder()
              .errorMessage("Invalid email address")
              .errorCode("InvalidParameterValue")
              .build();
      SesException sesException =
          (SesException) SesException.builder().awsErrorDetails(errorDetails).build();
      when(sesClient.sendEmail(any(SendEmailRequest.class))).thenThrow(sesException);

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(ExternalServiceException.class);
    }
  }
}
