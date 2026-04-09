package com.ebikes.notifications.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "cache.organization")
@Getter
@Setter
public class CacheProperties {

  private int ttlMinutes = 30;
  private String keyPrefix = "notifications";
}
