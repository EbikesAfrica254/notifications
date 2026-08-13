package com.ebikes.notifications.configurations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.ebikes.notifications.configurations.properties.NotificationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(
    prefix = "notification.channels.email",
    name = "provider",
    havingValue = "SMTP")
@RequiredArgsConstructor
@Slf4j
public class SmtpConfiguration {

  private final NotificationProperties notificationProperties;

  @Bean
  @ConditionalOnMissingBean(JavaMailSender.class)
  public JavaMailSender javaMailSender() {
    NotificationProperties.Smtp smtp = notificationProperties.getChannels().getEmail().getSmtp();

    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(smtp.getHost());
    mailSender.setPort(smtp.getPort());
    mailSender.setUsername(smtp.getUsername());
    mailSender.setPassword(smtp.getPassword());

    mailSender.getJavaMailProperties().put("mail.smtp.auth", "true");
    mailSender
        .getJavaMailProperties()
        .put("mail.smtp.ssl.enable", String.valueOf(smtp.isSslEnabled()));
    mailSender
        .getJavaMailProperties()
        .put("mail.smtp.connectiontimeout", String.valueOf(smtp.getConnectionTimeout()));
    mailSender.getJavaMailProperties().put("mail.smtp.timeout", String.valueOf(smtp.getTimeout()));
    mailSender
        .getJavaMailProperties()
        .put("mail.smtp.writetimeout", String.valueOf(smtp.getWriteTimeout()));

    log.info("JavaMailSender configured for SMTP host: {}:{}", smtp.getHost(), smtp.getPort());

    return mailSender;
  }
}
