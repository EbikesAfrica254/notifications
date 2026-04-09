package com.ebikes.notifications.adapters.channels.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.ebikes.notifications.configurations.properties.NotificationProperties;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Channels;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Sms;
import com.ebikes.notifications.configurations.properties.NotificationProperties.TaifaMobile;
import com.ebikes.notifications.dtos.requests.channels.sms.SmsRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.RateLimitException;

import tools.jackson.databind.ObjectMapper;

@DisplayName("TaifaMobileSmsAdapter")
@ExtendWith(MockitoExtension.class)
class TaifaMobileSmsAdapterTest {

  @Mock private NotificationProperties properties;
  @Mock private RestClient.Builder restClientBuilder;

  private TaifaMobileSmsAdapter adapter;

  // Chain mocks
  private RestClient restClient;
  private RestClient.ResponseSpec responseSpec;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SmsRequest request = new SmsRequest("Hello SMS", List.of("+254700000001"));

  @BeforeEach
  @SuppressWarnings({"unchecked", "rawtypes"})
  void setUp() {
    // Stub properties needed during buildRestClient()
    TaifaMobile taifaMobile = new TaifaMobile();
    taifaMobile.setApiKey("test-api-key");
    taifaMobile.setSenderName("TestSender");
    taifaMobile.setDefaultCost(new BigDecimal("0.75"));

    Sms sms = new Sms();
    sms.setTaifaMobile(taifaMobile);

    Channels channels = new Channels();
    channels.setSms(sms);

    when(properties.getChannels()).thenReturn(channels);

    // Mock the builder chain so buildRestClient() returns a mock RestClient
    restClient = mock(RestClient.class);
    when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
    when(restClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(restClientBuilder);
    when(restClientBuilder.requestFactory(any())).thenReturn(restClientBuilder);
    when(restClientBuilder.defaultStatusHandler(any(), any())).thenReturn(restClientBuilder);
    when(restClientBuilder.build()).thenReturn(restClient);

    adapter = new TaifaMobileSmsAdapter(properties, objectMapper, restClientBuilder);

    // Set up the post chain for send()
    RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
    responseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
  }

  @Nested
  @DisplayName("send")
  class Send {

    @Test
    @DisplayName("should return ChannelResponse on successful dispatch")
    @SuppressWarnings("unchecked")
    void shouldReturnChannelResponseOnSuccess() {
      String responseJson =
          "[{\"status_code\":\"1000\",\"status_desc\":\"Success\","
              + "\"message_id\":\"msg-001\",\"mobile_number\":\"+254700000001\","
              + "\"network_id\":\"1\",\"message_cost\":\"0.50\",\"credit_balance\":\"100.00\"}]";

      ResponseEntity<String> responseEntity = ResponseEntity.ok(responseJson);
      when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

      ChannelResponse result = adapter.send(request);

      assertThat(result.providerMessageId()).isEqualTo("msg-001");
      assertThat(result.costAmount()).isEqualByComparingTo(new BigDecimal("0.50"));
      assertThat(result.costCurrency()).isEqualTo("KES");
      assertThat(result.sentAt()).isNotNull();
    }

    @Test
    @DisplayName("should use default cost when message_cost is absent")
    @SuppressWarnings("unchecked")
    void shouldUseDefaultCostWhenMessageCostAbsent() {
      String responseJson =
          "[{\"status_code\":\"1000\",\"status_desc\":\"Success\","
              + "\"message_id\":\"msg-002\",\"mobile_number\":\"+254700000001\","
              + "\"network_id\":\"1\",\"message_cost\":null,\"credit_balance\":\"100.00\"}]";

      ResponseEntity<String> responseEntity = ResponseEntity.ok(responseJson);
      when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

      ChannelResponse result = adapter.send(request);

      // default cost (0.75) × 1 recipient = 0.75
      assertThat(result.costAmount()).isEqualByComparingTo(new BigDecimal("0.75"));
    }

    @Test
    @DisplayName("should throw ExternalServiceException when statusCode is not 1000")
    @SuppressWarnings("unchecked")
    void shouldThrowWhenStatusCodeIsNotSuccess() {
      String responseJson =
          "[{\"status_code\":\"2001\",\"status_desc\":\"Failed\","
              + "\"message_id\":null,\"mobile_number\":null,"
              + "\"network_id\":null,\"message_cost\":null,\"credit_balance\":null}]";

      ResponseEntity<String> responseEntity = ResponseEntity.ok(responseJson);
      when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should rethrow RateLimitException from status handler")
    @SuppressWarnings("unchecked")
    void shouldRethrowRateLimitException() {
      when(responseSpec.toEntity(String.class))
          .thenThrow(
              new RateLimitException(
                  com.ebikes.notifications.enums.ResponseCode.RATE_LIMIT_EXCEEDED, "rate limited"));

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(RateLimitException.class);
    }

    @Test
    @DisplayName("should rethrow ExternalServiceException from status handler")
    @SuppressWarnings("unchecked")
    void shouldRethrowExternalServiceException() {
      when(responseSpec.toEntity(String.class))
          .thenThrow(
              new ExternalServiceException(
                  "taifamobile://sms/sendsms",
                  "auth failed",
                  com.ebikes.notifications.enums.ResponseCode.AUTHENTICATION_FAILED));

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should wrap unexpected exception as ExternalServiceException")
    @SuppressWarnings("unchecked")
    void shouldWrapUnexpectedExceptionAsExternalServiceException() {
      when(responseSpec.toEntity(String.class)).thenThrow(new RuntimeException("unexpected"));

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(ExternalServiceException.class);
    }
  }
}
