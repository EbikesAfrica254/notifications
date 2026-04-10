package com.ebikes.notifications.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "services")
@Getter
@Setter
public class ServicesProperties {
  private ServiceConfiguration iam = new ServiceConfiguration();
  private ServiceConfiguration organizations = new ServiceConfiguration();

  @Data
  public static class ServiceConfiguration {
    private String baseUrl;
    private String clientRegistrationId;
  }
}
