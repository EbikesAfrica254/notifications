package com.ebikes.notifications.services.templates.variables.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.notifications.configurations.properties.ClientProperties;
import com.ebikes.notifications.configurations.properties.ClientProperties.AssetsConfiguration;
import com.ebikes.notifications.dtos.adapters.organizations.Organization;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.services.cache.OrganizationsCacheService;
import com.ebikes.notifications.support.fixtures.NotificationRequestFixtures;

@DisplayName("OrganizationVariableResolver")
@ExtendWith(MockitoExtension.class)
class OrganizationVariableResolverTest {

  private static final String FALLBACK_LOGO_URL = "https://assets.ebikes.test/logo.png";

  @Mock private ClientProperties clientProperties;
  @Mock private AssetsConfiguration assetsConfig;
  @Mock private OrganizationsCacheService organizationsCacheService;

  @InjectMocks private OrganizationVariableResolver resolver;

  @Nested
  @DisplayName("resolve → skips when organizationId absent")
  class SkipResolution {

    @Test
    @DisplayName("should return empty map when organizationId is null")
    void shouldReturnEmptyWhenOrganizationIdNull() {
      NotificationRequest request =
          NotificationRequestFixtures.organizationWelcomeWithoutOrganizationId();

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("resolve → organization not found")
  class OrganizationNotFound {

    @Test
    @DisplayName("should throw ExternalServiceException when organization not found")
    void shouldThrowWhenOrganizationNotFound() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();

      when(organizationsCacheService.findOrganizations(Set.of(request.organizationId())))
          .thenReturn(Map.of());

      assertThatThrownBy(() -> resolver.resolve(request))
          .isInstanceOf(ExternalServiceException.class)
          .hasMessageContaining(request.organizationId());
    }
  }

  @Nested
  @DisplayName("resolve → organization variables")
  class OrganizationVariables {

    @Test
    @DisplayName("should resolve organizationName and organizationAddress")
    void shouldResolveNameAndAddress() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();
      Organization org = organization(request.organizationId(), "https://logo.ebikes.test");

      when(organizationsCacheService.findOrganizations(Set.of(request.organizationId())))
          .thenReturn(Map.of(request.organizationId(), org));

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result)
          .containsEntry("organizationName", "eBikes Ltd")
          .containsEntry("organizationAddress", "123 Main St");
    }

    @Test
    @DisplayName("should resolve logoUrl from organization when present")
    void shouldResolveLogoUrlFromOrganization() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();
      Organization org = organization(request.organizationId(), "https://logo.ebikes.test");

      when(organizationsCacheService.findOrganizations(Set.of(request.organizationId())))
          .thenReturn(Map.of(request.organizationId(), org));

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).containsEntry("logoUrl", "https://logo.ebikes.test");
    }
  }

  @Nested
  @DisplayName("resolve → logoUrl fallback")
  class LogoUrlFallback {

    @Test
    @DisplayName("should use configured fallback when organization logoUrl is null")
    void shouldUseFallbackWhenLogoUrlNull() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();
      Organization org = organization(request.organizationId(), null);

      when(organizationsCacheService.findOrganizations(Set.of(request.organizationId())))
          .thenReturn(Map.of(request.organizationId(), org));
      when(clientProperties.getAssets()).thenReturn(assetsConfig);
      when(assetsConfig.getFallbackLogoUrl()).thenReturn(FALLBACK_LOGO_URL);

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).containsEntry("logoUrl", FALLBACK_LOGO_URL);
    }

    @Test
    @DisplayName(
        "should omit logoUrl when organization logoUrl is null and fallback not configured")
    void shouldOmitLogoUrlWhenNoFallback() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();
      Organization org = organization(request.organizationId(), null);

      when(organizationsCacheService.findOrganizations(Set.of(request.organizationId())))
          .thenReturn(Map.of(request.organizationId(), org));
      when(clientProperties.getAssets()).thenReturn(assetsConfig);
      when(assetsConfig.getFallbackLogoUrl()).thenReturn(null);

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).doesNotContainKey("logoUrl");
    }
  }

  private Organization organization(String id, String logoUrl) {
    return new Organization(id, "123 Main St", "eBikes Ltd", logoUrl);
  }
}
