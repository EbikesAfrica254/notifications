package com.ebikes.notifications.services.templates.variables.resolvers;

import java.io.Serializable;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ebikes.notifications.configurations.properties.ClientProperties;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationVariableResolver implements VariableResolver {

  private static final String CURRENT_YEAR = "currentYear";
  private static final String LOGIN_URL = "loginUrl";
  private static final String SUPPORT_URL = "supportUrl";
  private static final String SUPPORT_PATH = "/support";

  private final ClientProperties clientProperties;

  @Override
  public Map<String, Serializable> resolve(NotificationRequest request) {
    Map<String, Serializable> variables = new HashMap<>();

    variables.put(CURRENT_YEAR, String.valueOf(Year.now().getValue()));

    String clientBaseUrl = clientProperties.getClient().getBaseUrl();
    if (hasText(clientBaseUrl)) {
      variables.put(LOGIN_URL, clientBaseUrl);
      variables.put(SUPPORT_URL, normalise(clientBaseUrl) + SUPPORT_PATH);
    } else {
      log.warn("Client baseUrl is not configured - supportUrl will not be resolved");
    }

    log.debug("Resolved config variables: keys={}", variables.keySet());

    return variables;
  }

  private String normalise(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
