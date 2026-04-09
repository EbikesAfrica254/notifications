package com.ebikes.notifications.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "clients")
@Getter
@Setter
public class ClientProperties {
  private ClientConfiguration client;
  private OpsConfiguration ops;

  @Getter
  @Setter
  public static class ClientConfiguration {
    private String baseUrl;
    private String supportUrl;
  }

  @Getter
  @Setter
  public static class OpsConfiguration {
    private String baseUrl;
  }
}
