package com.ebikes.notifications.services.templates.variables.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.time.Year;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.notifications.configurations.properties.ClientProperties;
import com.ebikes.notifications.configurations.properties.ClientProperties.ClientConfiguration;
import com.ebikes.notifications.support.fixtures.NotificationRequestFixtures;

@DisplayName("ConfigurationVariableResolver")
@ExtendWith(MockitoExtension.class)
class ConfigurationVariableResolverTest {

  private static final String CLIENT_BASE_URL = "https://client.ebikes.test";

  @Mock private ClientProperties clientProperties;
  @Mock private ClientConfiguration clientConfig;

  @InjectMocks private ConfigurationVariableResolver resolver;

  @Nested
  @DisplayName("resolve → currentYear")
  class CurrentYear {

    @Test
    @DisplayName("should always resolve currentYear regardless of config")
    void shouldAlwaysResolveCurrentYear() {
      when(clientProperties.getClient()).thenReturn(clientConfig);
      when(clientConfig.getBaseUrl()).thenReturn(CLIENT_BASE_URL);

      Map<String, Serializable> result =
          resolver.resolve(NotificationRequestFixtures.organizationWelcome());

      assertThat(result).containsEntry("currentYear", String.valueOf(Year.now().getValue()));
    }
  }

  @Nested
  @DisplayName("resolve → loginUrl")
  class LoginUrl {

    @Test
    @DisplayName("should resolve loginUrl from client baseUrl")
    void shouldResolveLoginUrl() {
      when(clientProperties.getClient()).thenReturn(clientConfig);
      when(clientConfig.getBaseUrl()).thenReturn(CLIENT_BASE_URL);

      Map<String, Serializable> result =
          resolver.resolve(NotificationRequestFixtures.organizationWelcome());

      assertThat(result).containsEntry("loginUrl", CLIENT_BASE_URL);
    }

    @Test
    @DisplayName("should omit loginUrl when client baseUrl is null")
    void shouldOmitLoginUrlWhenBaseUrlNull() {
      when(clientProperties.getClient()).thenReturn(clientConfig);
      when(clientConfig.getBaseUrl()).thenReturn(null);

      Map<String, Serializable> result =
          resolver.resolve(NotificationRequestFixtures.organizationWelcome());

      assertThat(result).doesNotContainKey("loginUrl");
    }

    @Test
    @DisplayName("should omit loginUrl when client baseUrl is blank")
    void shouldOmitLoginUrlWhenBaseUrlBlank() {
      when(clientProperties.getClient()).thenReturn(clientConfig);
      when(clientConfig.getBaseUrl()).thenReturn("  ");

      Map<String, Serializable> result =
          resolver.resolve(NotificationRequestFixtures.organizationWelcome());

      assertThat(result).doesNotContainKey("loginUrl");
    }
  }

  @Nested
  @DisplayName("resolve → supportUrl")
  class SupportUrl {

    @Test
    @DisplayName("should resolve supportUrl by appending /support to client baseUrl")
    void shouldResolveSupportUrl() {
      when(clientProperties.getClient()).thenReturn(clientConfig);
      when(clientConfig.getBaseUrl()).thenReturn(CLIENT_BASE_URL);

      Map<String, Serializable> result =
          resolver.resolve(NotificationRequestFixtures.organizationWelcome());

      assertThat(result).containsEntry("supportUrl", CLIENT_BASE_URL + "/support");
    }

    @Test
    @DisplayName("should strip trailing slash before appending /support")
    void shouldNormaliseTrailingSlash() {
      when(clientProperties.getClient()).thenReturn(clientConfig);
      when(clientConfig.getBaseUrl()).thenReturn(CLIENT_BASE_URL + "/");

      Map<String, Serializable> result =
          resolver.resolve(NotificationRequestFixtures.organizationWelcome());

      assertThat(result).containsEntry("supportUrl", CLIENT_BASE_URL + "/support");
    }

    @Test
    @DisplayName("should omit supportUrl when client baseUrl is blank")
    void shouldOmitSupportUrlWhenBaseUrlBlank() {
      when(clientProperties.getClient()).thenReturn(clientConfig);
      when(clientConfig.getBaseUrl()).thenReturn("");

      Map<String, Serializable> result =
          resolver.resolve(NotificationRequestFixtures.organizationWelcome());

      assertThat(result).doesNotContainKey("supportUrl");
    }
  }
}
