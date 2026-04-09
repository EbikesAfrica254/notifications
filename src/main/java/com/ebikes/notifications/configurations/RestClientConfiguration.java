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

import com.ebikes.notifications.configurations.properties.OrganizationServiceProperties;

@Configuration
public class RestClientConfiguration {

  @Bean
  public RestClient organizationServiceRestClient(
      RestClient.Builder restClientBuilder,
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientService authorizedClientService,
      OrganizationServiceProperties organizationServiceProperties) {

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
    requestInterceptor.setClientRegistrationIdResolver(
        request -> organizationServiceProperties.getClientRegistrationId());

    return restClientBuilder
        .baseUrl(organizationServiceProperties.getBaseUrl())
        .requestInterceptor(requestInterceptor)
        .build();
  }
}
