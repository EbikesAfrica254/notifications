package com.ebikes.notifications.configurations.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "aws")
@Data
@Validated
public class AwsProperties {

  @NotNull private Credentials credentials;

  @Data
  public static class Credentials {
    @NotBlank private String accessKey;

    @NotBlank private String secretKey;
  }
}
