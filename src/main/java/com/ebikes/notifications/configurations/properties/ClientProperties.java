package com.ebikes.notifications.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "clients")
@Data
public class ClientProperties {

  private AssetsConfiguration assets = new AssetsConfiguration();
  private ClientConfiguration client = new ClientConfiguration();
  private ClientConfiguration ops = new ClientConfiguration();

  @Data
  public static class AssetsConfiguration {
    private String fallbackLogoUrl;
  }

  @Data
  public static class ClientConfiguration {
    private String baseUrl;
  }
}
