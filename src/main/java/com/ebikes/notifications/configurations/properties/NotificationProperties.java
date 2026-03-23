package com.ebikes.notifications.configurations.properties;

import java.math.BigDecimal;
import java.time.Duration;

import jakarta.validation.Valid;
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
    private boolean enabled = true;

    @Min(value = 1, message = "Rate limit must be positive") private int rateLimit = 500;

    @Valid private Ses ses = new Ses();
  }

  @Data
  public static class Ses {
    private String endpoint;

    @NotBlank(message = "SES region is required") private String region;

    @NotBlank(message = "SES sender address is required") private String senderAddress;
  }

  @Data
  public static class Sms {
    @Valid @NotNull private TaifaMobile taifaMobile = new TaifaMobile();
    private boolean enabled = true;

    @Min(value = 1, message = "Rate limit must be positive") private int rateLimit = 100;
  }

  @Data
  public static class Sse {
    @Min(value = 1, message = "Connection limit must be positive") private int connectionLimit = 1000;

    private Long connectionTimeout;
    private boolean enabled = true;
    private Duration heartbeatInterval = Duration.ofSeconds(30);

    @Min(value = 1, message = "Rate limit must be positive") private int rateLimit = 1000;
  }

  @Data
  public static class TaifaMobile {
    @NotBlank(message = "Taifa Mobile API key is required") private String apiKey;

    @NotBlank(message = "Taifa Mobile sender name is required") private String senderName;

    @DecimalMin(value = "0.0", inclusive = false) private BigDecimal defaultCost = new BigDecimal("0.75");
  }

  @Data
  public static class Whatsapp {

    private boolean enabled = true;

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
