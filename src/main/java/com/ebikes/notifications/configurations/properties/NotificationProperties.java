package com.ebikes.notifications.configurations.properties;

import java.math.BigDecimal;
import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@ConfigurationProperties(prefix = "notification")
@Component
@Data
@EnableConfigurationProperties
@Validated
public class NotificationProperties {

  @Valid @NotNull private Channels channels = new Channels();

  @Data
  public static class Channels {
    @Valid @NotNull private Email email = new Email();
    @Valid @NotNull private Sms sms = new Sms();
    @Valid @NotNull private Sse sse = new Sse();
    @Valid private Whatsapp whatsapp = new Whatsapp();
  }

  @Data
  public static class Email {
    private boolean enabled;

    @NotNull(message = "Email provider is required") private EmailProvider provider = EmailProvider.SES;

    @Min(value = 1, message = "Rate limit must be positive") private int rateLimit = 500;

    @Valid private Ses ses = new Ses();
    @Valid private Smtp smtp = new Smtp();

    @AssertTrue(message = "SES configuration is required when provider is SES")
    private boolean isSesConfigValid() {
      if (provider != EmailProvider.SES) {
        return true;
      }
      return ses != null
          && ses.getRegion() != null
          && !ses.getRegion().isBlank()
          && ses.getSenderAddress() != null
          && !ses.getSenderAddress().isBlank();
    }

    @AssertTrue(message = "SMTP configuration is required when provider is SMTP")
    private boolean isSmtpConfigValid() {
      if (provider != EmailProvider.SMTP) {
        return true;
      }
      return smtp != null
          && smtp.getHost() != null
          && !smtp.getHost().isBlank()
          && smtp.getUsername() != null
          && !smtp.getUsername().isBlank()
          && smtp.getPassword() != null
          && !smtp.getPassword().isBlank()
          && smtp.getSenderAddress() != null
          && !smtp.getSenderAddress().isBlank();
    }
  }

  public enum EmailProvider {
    SES,
    SMTP
  }

  @Data
  public static class Ses {
    private String endpoint;
    private String region;
    private String senderAddress;
  }

  @Data
  public static class Sms {
    private boolean enabled;

    @Min(value = 1, message = "Rate limit must be positive") private int rateLimit = 100;

    @Valid @NotNull private TaifaMobile taifaMobile = new TaifaMobile();
  }

  @Data
  public static class Smtp {
    private int connectionTimeout = 5000;
    private String host;
    private String password;
    private int port;
    private String senderAddress;
    private boolean sslEnabled = true;
    private int timeout = 3000;
    private String username;
    private int writeTimeout = 5000;
  }

  @Data
  public static class Sse {
    @Min(value = 1, message = "Connection limit must be positive") private int connectionLimit = 1000;

    private Long connectionTimeout;
    private boolean enabled;
    private Duration heartbeatInterval = Duration.ofSeconds(30);

    @Min(value = 1, message = "Rate limit must be positive") private int rateLimit = 1000;
  }

  @Data
  public static class TaifaMobile {
    @NotBlank(message = "Taifa Mobile API key is required") private String apiKey;

    @DecimalMin(value = "0.0", inclusive = false) private BigDecimal defaultCost = new BigDecimal("0.75");

    @NotBlank(message = "Taifa Mobile sender name is required") private String senderName;
  }

  @Data
  public static class Whatsapp {

    private boolean enabled;

    @Valid private Meta meta = new Meta();

    @Min(value = 1) private int rateLimit = 80;

    @Data
    public static class Meta {
      @NotBlank private String apiVersion;
      @NotBlank private String appId;
      @NotBlank private String appSecret;
      @NotBlank private String phoneNumberId;
      @NotBlank private String systemUserToken;
    }
  }
}
