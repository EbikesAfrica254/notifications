package com.ebikes.notifications.adapters.sms;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ebikes.notifications.configurations.properties.NotificationProperties;
import com.ebikes.notifications.constants.ApplicationConstants;
import com.ebikes.notifications.dtos.requests.channels.providers.TaifaMobileRequest;
import com.ebikes.notifications.dtos.requests.channels.sms.SmsRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.dtos.responses.channels.providers.TaifaMobileResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.RateLimitException;
import com.ebikes.notifications.support.json.JsonNodeUtilities;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class TaifaMobileSmsAdapter implements SmsAdapter {

  private static final String COST_CURRENCY = "KES";
  private static final String ENDPOINT =
      ApplicationConstants.TaifaMobile.ENDPOINT_PREFIX
          + ApplicationConstants.TaifaMobile.SEND_SMS_PATH;

  private final NotificationProperties properties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;

  public TaifaMobileSmsAdapter(
      NotificationProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.restClient = buildRestClient(builder);
  }

  @Override
  public ChannelType getChannelType() {
    return ChannelType.SMS;
  }

  @Override
  public ChannelResponse send(SmsRequest request) {
    try {
      String senderName = properties.getChannels().getSms().getTaifaMobile().getSenderName();
      String mobile = String.join(",", request.recipients());

      log.debug(
          "Sending SMS via Taifa Mobile - recipients={} mobile={}",
          request.recipients().size(),
          mobile);

      TaifaMobileRequest taifaRequest =
          new TaifaMobileRequest(mobile, "json", senderName, 0, request.body(), null);

      ResponseEntity<String> responseEntity =
          restClient
              .post()
              .uri(ApplicationConstants.TaifaMobile.SEND_SMS_PATH)
              .body(taifaRequest)
              .retrieve()
              .toEntity(String.class);

      TaifaMobileResponse response = parseResponse(responseEntity.getBody());

      if (!"1000".equals(response.statusCode())) {
        log.error(
            "Taifa Mobile SMS dispatch failed - statusCode={} statusDesc={}",
            response.statusCode(),
            response.statusDesc());
        throw new ExternalServiceException(
            ENDPOINT,
            response.statusDesc() != null ? response.statusDesc() : "SMS dispatch failed",
            ResponseCode.EXTERNAL_SERVICE_ERROR);
      }

      BigDecimal cost = parseCost(response, request.recipients().size());

      log.info(
          "SMS dispatched via Taifa Mobile - messageId={} recipients={} cost={} creditBalance={}",
          response.messageId(),
          request.recipients().size(),
          cost,
          response.creditBalance());

      return new ChannelResponse(
          cost,
          COST_CURRENCY,
          Map.of(
              "status",
              "dispatched",
              "recipientCount",
              request.recipients().size(),
              "creditBalance",
              response.creditBalance() != null ? response.creditBalance() : "unknown"),
          response.messageId(),
          ENDPOINT,
          OffsetDateTime.now());

    } catch (RateLimitException | ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Taifa Mobile SMS unexpected error - recipients={}", request.recipients().size(), e);
      throw new ExternalServiceException(
          ENDPOINT,
          e.getMessage() != null ? e.getMessage() : "SMS processing failed",
          ResponseCode.EXTERNAL_SERVICE_ERROR,
          e);
    }
  }

  private RestClient buildRestClient(RestClient.Builder builder) {
    String apiKey = properties.getChannels().getSms().getTaifaMobile().getApiKey();

    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(
                    Duration.ofSeconds(ApplicationConstants.HttpClient.CONNECT_TIMEOUT_SECONDS))
                .build());
    factory.setReadTimeout(
        Duration.ofSeconds(ApplicationConstants.HttpClient.READ_TIMEOUT_SECONDS));

    return builder
        .baseUrl(ApplicationConstants.TaifaMobile.BASE_URL)
        .defaultHeader("api-key", apiKey)
        .defaultHeader("Content-Type", "application/json")
        .requestFactory(factory)
        .defaultStatusHandler(
            HttpStatusCode::isError,
            (request, response) -> {
              int statusCode = response.getStatusCode().value();
              String endpoint =
                  ApplicationConstants.TaifaMobile.ENDPOINT_PREFIX + request.getURI().getPath();
              String responseBody =
                  new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);

              TaifaMobileError error = parseTaifaMobileError(responseBody);

              if (statusCode == 429 || isLowCreditsError(error.statusCode())) {
                throw new RateLimitException(
                    ResponseCode.RATE_LIMIT_EXCEEDED,
                    error.statusDesc() != null ? error.statusDesc() : "SMS rate limit exceeded");
              }

              if (isAuthenticationError(error.statusCode(), statusCode)) {
                throw new ExternalServiceException(
                    endpoint,
                    "Taifa Mobile authentication failed: " + error.statusDesc(),
                    ResponseCode.AUTHENTICATION_FAILED);
              }

              if (isConfigurationError(error.statusCode())) {
                throw new ExternalServiceException(
                    endpoint, error.statusDesc(), ResponseCode.INVALID_ARGUMENTS);
              }

              throw new ExternalServiceException(
                  endpoint,
                  error.statusDesc() != null ? error.statusDesc() : "Taifa Mobile API error",
                  ResponseCode.fromHttpStatus(statusCode));
            })
        .build();
  }

  private TaifaMobileResponse parseResponse(String responseBody) {
    try {
      JsonNode entry = JsonNodeUtilities.extractFirstEntry(objectMapper, responseBody);
      return new TaifaMobileResponse(
          JsonNodeUtilities.stringValueOrNull(entry.path("status_code")),
          JsonNodeUtilities.stringValueOrNull(entry.path("status_desc")),
          JsonNodeUtilities.stringValueOrNull(entry.path("message_id")),
          JsonNodeUtilities.stringValueOrNull(entry.path("mobile_number")),
          JsonNodeUtilities.stringValueOrNull(entry.path("network_id")),
          JsonNodeUtilities.stringValueOrNull(entry.path("message_cost")),
          JsonNodeUtilities.stringValueOrNull(entry.path("credit_balance")));
    } catch (Exception e) {
      log.error("Failed to parse Taifa Mobile response", e);
      throw new ExternalServiceException(
          ENDPOINT,
          "Failed to parse Taifa Mobile response",
          ResponseCode.EXTERNAL_SERVICE_ERROR,
          e);
    }
  }

  private TaifaMobileError parseTaifaMobileError(String responseBody) {
    try {
      JsonNode entry = JsonNodeUtilities.extractFirstEntry(objectMapper, responseBody);
      return new TaifaMobileError(
          JsonNodeUtilities.stringValueOrNull(entry.path("status_code")),
          JsonNodeUtilities.stringValueOrNull(entry.path("status_desc")));
    } catch (Exception e) {
      log.debug("Failed to parse Taifa Mobile error response", e);
      return new TaifaMobileError(null, responseBody);
    }
  }

  private boolean isAuthenticationError(String taifaStatusCode, int httpStatusCode) {
    if (httpStatusCode == 401 || httpStatusCode == 403) {
      return true;
    }
    return ApplicationConstants.TaifaMobile.STATUS_AUTH_INVALID_ACCOUNT.equals(taifaStatusCode)
        || ApplicationConstants.TaifaMobile.STATUS_AUTH_IP_NOT_WHITELISTED.equals(taifaStatusCode);
  }

  private boolean isConfigurationError(String taifaStatusCode) {
    return ApplicationConstants.TaifaMobile.STATUS_AUTH_INVALID_SENDER.equals(taifaStatusCode)
        || ApplicationConstants.TaifaMobile.STATUS_AUTH_INVALID_API_KEY.equals(taifaStatusCode)
        || ApplicationConstants.TaifaMobile.STATUS_AUTH_INVALID_ACCOUNT.equals(taifaStatusCode);
  }

  private boolean isLowCreditsError(String taifaStatusCode) {
    return ApplicationConstants.TaifaMobile.STATUS_LOW_CREDITS.equals(taifaStatusCode);
  }

  private BigDecimal parseCost(TaifaMobileResponse response, int recipientCount) {
    if (response.messageCost() != null) {
      try {
        return new BigDecimal(response.messageCost());
      } catch (NumberFormatException e) {
        log.warn("Unparseable message_cost from Taifa Mobile: {}", response.messageCost());
      }
    }

    BigDecimal defaultCost = properties.getChannels().getSms().getTaifaMobile().getDefaultCost();
    return defaultCost.multiply(BigDecimal.valueOf(recipientCount));
  }

  private record TaifaMobileError(String statusCode, String statusDesc) {}
}
