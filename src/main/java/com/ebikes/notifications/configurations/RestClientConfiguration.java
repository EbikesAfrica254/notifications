package com.ebikes.notifications.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import com.ebikes.notifications.configurations.properties.ServicesProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RestClientConfiguration {

  private final ServicesProperties servicesProperties;

  @Bean
  public RestClient organizationServiceRestClient(
      RestClient.Builder restClientBuilder,
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientService authorizedClientService) {
    return buildRestClient(
        restClientBuilder,
        clientRegistrationRepository,
        authorizedClientService,
        servicesProperties.getOrganizations());
  }

  @Bean
  public RestClient iamServiceRestClient(
      RestClient.Builder restClientBuilder,
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientService authorizedClientService) {
    return buildRestClient(
        restClientBuilder,
        clientRegistrationRepository,
        authorizedClientService,
        servicesProperties.getIam());
  }

  private RestClient buildRestClient(
      RestClient.Builder restClientBuilder,
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientService authorizedClientService,
      ServicesProperties.ServiceConfiguration config) {
    OAuth2AuthorizedClientProvider authorizedClientProvider =
        OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
    AuthorizedClientServiceOAuth2AuthorizedClientManager clientManager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService);
    clientManager.setAuthorizedClientProvider(authorizedClientProvider);
    OAuth2ClientHttpRequestInterceptor requestInterceptor =
        new OAuth2ClientHttpRequestInterceptor(clientManager);
    requestInterceptor.setAuthorizationFailureHandler(
        OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(authorizedClientService));
    requestInterceptor.setClientRegistrationIdResolver(request -> config.getClientRegistrationId());
    return restClientBuilder
        .baseUrl(config.getBaseUrl())
        .requestInterceptor(requestInterceptor)
        .build();
  }
}
