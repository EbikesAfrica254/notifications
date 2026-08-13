package com.ebikes.notifications.adapters.channels.email;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.mail.internet.MimeMessage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ebikes.notifications.configurations.properties.NotificationProperties;
import com.ebikes.notifications.dtos.requests.channels.email.EmailRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.support.references.ReferenceGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(
    prefix = "notification.channels.email",
    name = "provider",
    havingValue = "SMTP")
@RequiredArgsConstructor
@Service
@Slf4j
public class SmtpEmailAdapter implements EmailAdapter {

  private static final BigDecimal COST_PER_EMAIL = BigDecimal.ZERO;
  private static final String COST_CURRENCY = "KES";
  private static final String ENDPOINT = "smtp://send-email";

  private final JavaMailSender mailSender;
  private final NotificationProperties properties;

  @Override
  public ChannelType getChannelType() {
    return ChannelType.EMAIL;
  }

  @Override
  public ChannelResponse send(EmailRequest request) throws ExternalServiceException {
    try {
      log.debug("Sending email via SMTP - recipient={}", request.recipient());

      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
      helper.setFrom(properties.getChannels().getEmail().getSmtp().getSenderAddress());
      helper.setTo(request.recipient());
      helper.setSubject(request.subject());
      helper.setText(request.body(), true);

      mailSender.send(mimeMessage);

      String messageId = ReferenceGenerator.generateMessageReference(ChannelType.EMAIL);

      log.info(
          "Email sent via SMTP - messageId={} recipient={} cost={}",
          messageId,
          request.recipient(),
          COST_PER_EMAIL);

      return new ChannelResponse(
          COST_PER_EMAIL,
          COST_CURRENCY,
          Map.of("provider", "smtp"),
          messageId,
          ENDPOINT,
          OffsetDateTime.now());

    } catch (MailAuthenticationException e) {
      log.error("SMTP authentication failed - recipient={}", request.recipient(), e);

      throw new ExternalServiceException(
          ENDPOINT, e.getMessage(), ResponseCode.AUTHENTICATION_FAILED, e);

    } catch (MailException | jakarta.mail.MessagingException e) {
      log.error(
          "SMTP email failed - recipient={} error={}", request.recipient(), e.getMessage(), e);

      throw new ExternalServiceException(
          ENDPOINT, e.getMessage(), ResponseCode.EXTERNAL_SERVICE_ERROR, e);
    }
  }
}
