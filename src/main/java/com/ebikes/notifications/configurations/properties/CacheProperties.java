package com.ebikes.notifications.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "cache")
@Getter
@Setter
public class CacheProperties {
  private CacheConfiguration iam = new CacheConfiguration();
  private CacheConfiguration organizations = new CacheConfiguration();

  @Data
  public static class CacheConfiguration {
    private String keyPrefix = "notifications";
    private int ttlMinutes = 30;
  }
}
