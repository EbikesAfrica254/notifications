package com.ebikes.notifications.adapters.channels.email;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ebikes.notifications.configurations.properties.NotificationProperties;
import com.ebikes.notifications.dtos.requests.channels.email.EmailRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SesEmailAdapter implements EmailAdapter {

  private static final String ENDPOINT = "ses://send-email";
  private static final BigDecimal COST_PER_EMAIL = new BigDecimal("0.0001");
  private static final String COST_CURRENCY = "USD";

  private final NotificationProperties properties;
  private final SesClient sesClient;

  @Override
  public ChannelType getChannelType() {
    return ChannelType.EMAIL;
  }

  @Override
  public ChannelResponse send(EmailRequest request) throws ExternalServiceException {
    try {
      log.debug("Sending email via SES - recipient={}", request.recipient());

      SendEmailRequest sesRequest =
          SendEmailRequest.builder()
              .destination(d -> d.toAddresses(request.recipient()))
              .message(
                  m ->
                      m.subject(c -> c.data(request.subject()))
                          .body(b -> b.html(c -> c.data(request.body()))))
              .source(properties.getChannels().getEmail().getSes().getSenderAddress())
              .build();

      SendEmailResponse response = sesClient.sendEmail(sesRequest);

      log.info(
          "Email sent via SES - messageId={} recipient={} cost={}",
          response.messageId(),
          request.recipient(),
          COST_PER_EMAIL);

      return new ChannelResponse(
          COST_PER_EMAIL,
          COST_CURRENCY,
          Map.of("provider", "aws-ses"),
          response.messageId(),
          ENDPOINT,
          OffsetDateTime.now());

    } catch (SesException e) {
      String errorMessage = e.awsErrorDetails().errorMessage();

      log.error("SES email failed - recipient={} error={}", request.recipient(), errorMessage, e);

      throw new ExternalServiceException(
          ENDPOINT, errorMessage, ResponseCode.EXTERNAL_SERVICE_ERROR, e);
    }
  }
}
