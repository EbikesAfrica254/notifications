package com.ebikes.notifications.adapters.channels.whatsapp;

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
import org.springframework.web.client.RestClient;

import com.ebikes.notifications.configurations.properties.NotificationProperties;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Channels;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Whatsapp;
import com.ebikes.notifications.configurations.properties.NotificationProperties.Whatsapp.Meta;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppOutboundRequest;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.enums.WhatsAppMessageType;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.RateLimitException;
import com.ebikes.notifications.mappers.WhatsAppMapper;

@DisplayName("MetaWhatsAppAdapter")
@ExtendWith(MockitoExtension.class)
class MetaWhatsAppAdapterTest {

  @Mock private WhatsAppMapper mapper;
  @Mock private NotificationProperties properties;
  @Mock private RestClient.Builder restClientBuilder;

  private RestClient restClient;
  private RestClient.ResponseSpec responseSpec;

  private final WhatsAppRequest request =
      new WhatsAppRequest(
          "Hello",
          null,
          null,
          null,
          null,
          null,
          null,
          WhatsAppMessageType.TEXT,
          null,
          null,
          "+254700000001",
          null);

  private Channels buildChannels(String token) {
    Meta meta = new Meta();
    meta.setApiVersion("v19.0");
    meta.setPhoneNumberId("phone-123");
    meta.setSystemUserToken(token);
    meta.setAppId("app-id");
    meta.setAppSecret("app-secret");

    Whatsapp whatsapp = new Whatsapp();
    whatsapp.setMeta(meta);

    Channels channels = new Channels();
    channels.setWhatsapp(whatsapp);
    return channels;
  }

  private MetaWhatsAppAdapter buildAdapter() {
    restClient = mock(RestClient.class);
    when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
    when(restClientBuilder.requestFactory(any())).thenReturn(restClientBuilder);
    when(restClientBuilder.defaultStatusHandler(any(), any())).thenReturn(restClientBuilder);
    when(restClientBuilder.build()).thenReturn(restClient);

    MetaWhatsAppAdapter adapter =
        new MetaWhatsAppAdapter(mapper, properties, null, restClientBuilder);

    RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
    responseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
    when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);

    when(mapper.toOutboundRequest(any(WhatsAppRequest.class)))
        .thenReturn(mock(WhatsAppOutboundRequest.class));

    return adapter;
  }

  @Nested
  @DisplayName("send")
  class Send {

    private MetaWhatsAppAdapter adapter;

    @BeforeEach
    void setUp() {
      when(properties.getChannels()).thenReturn(buildChannels("test-token"));
      adapter = buildAdapter();
    }

    @Test
    @DisplayName("should return ChannelResponse with messageId on success")
    void shouldReturnChannelResponseOnSuccess() {
      MetaWhatsAppAdapter.MetaApiResponse response =
          new MetaWhatsAppAdapter.MetaApiResponse(
              List.of(new MetaWhatsAppAdapter.MetaApiResponse.Message("wamid-001")));
      when(responseSpec.body(MetaWhatsAppAdapter.MetaApiResponse.class)).thenReturn(response);

      ChannelResponse result = adapter.send(request);

      assertThat(result.providerMessageId()).isEqualTo("wamid-001");
      assertThat(result.costAmount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.costCurrency()).isEqualTo("KES");
      assertThat(result.providerName()).isEqualTo("meta://send-whatsapp-message");
      assertThat(result.sentAt()).isNotNull();
    }

    @Test
    @DisplayName("should include message type in metadata")
    void shouldIncludeMessageTypeInMetadata() {
      MetaWhatsAppAdapter.MetaApiResponse response =
          new MetaWhatsAppAdapter.MetaApiResponse(
              List.of(new MetaWhatsAppAdapter.MetaApiResponse.Message("wamid-001")));
      when(responseSpec.body(MetaWhatsAppAdapter.MetaApiResponse.class)).thenReturn(response);

      ChannelResponse result = adapter.send(request);

      assertThat(result.metadata()).containsEntry("type", "TEXT");
    }

    @Test
    @DisplayName("should throw ExternalServiceException when response is null")
    void shouldThrowWhenResponseIsNull() {
      when(responseSpec.body(MetaWhatsAppAdapter.MetaApiResponse.class)).thenReturn(null);

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should throw ExternalServiceException when messages list is empty")
    void shouldThrowWhenMessagesListIsEmpty() {
      MetaWhatsAppAdapter.MetaApiResponse response =
          new MetaWhatsAppAdapter.MetaApiResponse(List.of());
      when(responseSpec.body(MetaWhatsAppAdapter.MetaApiResponse.class)).thenReturn(response);

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should rethrow RateLimitException from status handler")
    void shouldRethrowRateLimitException() {
      when(responseSpec.body(MetaWhatsAppAdapter.MetaApiResponse.class))
          .thenThrow(new RateLimitException(ResponseCode.RATE_LIMIT_EXCEEDED, "rate limited"));

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(RateLimitException.class);
    }

    @Test
    @DisplayName("should rethrow ExternalServiceException from status handler")
    void shouldRethrowExternalServiceException() {
      when(responseSpec.body(MetaWhatsAppAdapter.MetaApiResponse.class))
          .thenThrow(
              new ExternalServiceException(
                  "meta://messages", "auth failed", ResponseCode.AUTHENTICATION_FAILED));

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("resolveToken")
  class ResolveToken {

    @Test
    @DisplayName("should throw IllegalStateException when system user token is blank")
    void shouldThrowWhenTokenIsBlank() {
      when(properties.getChannels()).thenReturn(buildChannels("   "));

      // buildAdapter() registers post-chain stubs — resolveToken throws before any are used,
      // so we construct the adapter without them
      restClient = mock(RestClient.class);
      when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
      when(restClientBuilder.requestFactory(any())).thenReturn(restClientBuilder);
      when(restClientBuilder.defaultStatusHandler(any(), any())).thenReturn(restClientBuilder);
      when(restClientBuilder.build()).thenReturn(restClient);

      MetaWhatsAppAdapter adapter =
          new MetaWhatsAppAdapter(mapper, properties, null, restClientBuilder);

      assertThatThrownBy(() -> adapter.send(request)).isInstanceOf(IllegalStateException.class);
    }
  }
}
