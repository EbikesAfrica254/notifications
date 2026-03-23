package com.ebikes.notifications.adapters.whatsapp;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ebikes.notifications.configurations.properties.NotificationProperties;
import com.ebikes.notifications.constants.ApplicationConstants;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppOutboundRequest;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.RateLimitException;
import com.ebikes.notifications.mappers.WhatsAppMapper;
import com.ebikes.notifications.support.json.JsonNodeUtilities;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class MetaWhatsAppAdapter implements WhatsAppAdapter {

  private static final String COST_CURRENCY = "KES";
  private static final String ENDPOINT = "meta://send-whatsapp-message";

  private final WhatsAppMapper mapper;
  private final NotificationProperties properties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;

  public MetaWhatsAppAdapter(
      WhatsAppMapper mapper,
      NotificationProperties properties,
      ObjectMapper objectMapper,
      RestClient.Builder builder) {
    this.mapper = mapper;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.restClient = buildRestClient(builder);
  }

  @Override
  public ChannelType getChannelType() {
    return ChannelType.WHATSAPP;
  }

  @Override
  public ChannelResponse send(WhatsAppRequest request) {
    String token = resolveToken();
    String apiVersion = properties.getChannels().getWhatsapp().getMeta().getApiVersion();
    String phoneNumberId = properties.getChannels().getWhatsapp().getMeta().getPhoneNumberId();

    WhatsAppOutboundRequest payload = mapper.toOutboundRequest(request);

    MetaApiResponse response =
        restClient
            .post()
            .uri("/{version}/{phoneId}/messages", apiVersion, phoneNumberId)
            .header("Authorization", "Bearer " + token)
            .body(payload)
            .retrieve()
            .body(MetaApiResponse.class);

    if (response == null || response.messages() == null || response.messages().isEmpty()) {
      throw new ExternalServiceException(
          ENDPOINT,
          "Meta WhatsApp API returned empty response",
          ResponseCode.EXTERNAL_SERVICE_ERROR);
    }

    String messageId = response.messages().getFirst().id();

    log.debug("WhatsApp message sent - messageId={} recipient={}", messageId, request.to());

    return new ChannelResponse(
        BigDecimal.ZERO,
        COST_CURRENCY,
        Map.of("type", request.messageType().name()),
        messageId,
        ENDPOINT,
        OffsetDateTime.now());
  }

  private String resolveToken() {
    String token = properties.getChannels().getWhatsapp().getMeta().getSystemUserToken();
    if (token == null || token.isBlank()) {
      throw new IllegalStateException(
          "WhatsApp System User Token is not configured. "
              + "Please set META_SYSTEM_USER_TOKEN environment variable.");
    }
    return token;
  }

  private RestClient buildRestClient(RestClient.Builder builder) {
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(
                    Duration.ofSeconds(
                        ApplicationConstants.HttpClient.WHATSAPP_CONNECT_TIMEOUT_SECONDS))
                .build());
    factory.setReadTimeout(
        Duration.ofSeconds(ApplicationConstants.HttpClient.WHATSAPP_READ_TIMEOUT_SECONDS));

    return builder
        .baseUrl(ApplicationConstants.WhatsApp.BASE_URL)
        .requestFactory(factory)
        .defaultStatusHandler(
            HttpStatusCode::isError,
            (request, response) -> {
              int statusCode = response.getStatusCode().value();
              String endpoint =
                  ApplicationConstants.WhatsApp.ENDPOINT_PREFIX + request.getURI().getPath();
              String responseBody =
                  new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);

              MetaError error = parseMetaError(responseBody);

              if (isRateLimitError(error.code(), statusCode)) {
                throw new RateLimitException(
                    ResponseCode.RATE_LIMIT_EXCEEDED,
                    error.message() != null ? error.message() : "WhatsApp rate limit exceeded");
              }

              if (isAuthenticationError(error.code(), error.type())) {
                throw new ExternalServiceException(
                    endpoint,
                    "WhatsApp authentication failed: " + error.message(),
                    ResponseCode.AUTHENTICATION_FAILED);
              }

              throw new ExternalServiceException(
                  endpoint,
                  error.message() != null ? error.message() : "WhatsApp API error",
                  ResponseCode.fromHttpStatus(statusCode));
            })
        .build();
  }

  private MetaError parseMetaError(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode errorNode = root.path("error");
      if (!errorNode.isMissingNode()) {
        return new MetaError(
            errorNode.path("code").asInt(0),
            JsonNodeUtilities.stringValueOrNull(errorNode.path("message")),
            JsonNodeUtilities.stringValueOrNull(errorNode.path("type")));
      }
    } catch (Exception e) {
      log.debug("Failed to parse Meta error response", e);
    }
    return new MetaError(0, responseBody, null);
  }

  private boolean isRateLimitError(int errorCode, int statusCode) {
    return statusCode == 429
        || errorCode == ApplicationConstants.WhatsApp.RATE_LIMIT_APP
        || errorCode == ApplicationConstants.WhatsApp.RATE_LIMIT_BUSINESS
        || errorCode == ApplicationConstants.WhatsApp.RATE_LIMIT_PER_USER;
  }

  private boolean isAuthenticationError(int errorCode, String type) {
    return errorCode == ApplicationConstants.WhatsApp.AUTH_ERROR_CODE
        || ApplicationConstants.WhatsApp.AUTH_EXCEPTION_TYPE.equals(type);
  }

  private record MetaError(int code, String message, String type) {}

  private record MetaApiResponse(List<Message> messages) {
    private record Message(String id) {}
  }
}
