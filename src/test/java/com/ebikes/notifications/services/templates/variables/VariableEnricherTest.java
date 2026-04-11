package com.ebikes.notifications.services.templates.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.notifications.configurations.properties.ClientProperties;
import com.ebikes.notifications.configurations.properties.ClientProperties.AssetsConfiguration;
import com.ebikes.notifications.configurations.properties.ClientProperties.ClientConfiguration;
import com.ebikes.notifications.database.models.TemplateVariable;
import com.ebikes.notifications.dtos.adapters.iam.UserDetails;
import com.ebikes.notifications.dtos.adapters.organizations.Organization;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.enums.VariableType;
import com.ebikes.notifications.services.cache.IamCacheService;
import com.ebikes.notifications.services.cache.OrganizationsCacheService;
import com.ebikes.notifications.services.templates.variables.resolvers.ConfigurationVariableResolver;
import com.ebikes.notifications.services.templates.variables.resolvers.OrganizationVariableResolver;
import com.ebikes.notifications.services.templates.variables.resolvers.UserVariableResolver;
import com.ebikes.notifications.support.fixtures.NotificationRequestFixtures;

@DisplayName("VariableEnricher")
@ExtendWith(MockitoExtension.class)
class VariableEnricherTest {

  private static final String CLIENT_BASE_URL = "https://client.ebikes.test";
  private static final String FALLBACK_LOGO_URL = "https://assets.ebikes.test/logo.png";

  @Mock private ClientProperties clientProperties;
  @Mock private ClientConfiguration clientConfig;
  @Mock private AssetsConfiguration assetsConfig;
  @Mock private IamCacheService iamCacheService;
  @Mock private OrganizationsCacheService organizationsCacheService;

  private VariableEnricher enricher() {
    ConfigurationVariableResolver configResolver =
        new ConfigurationVariableResolver(clientProperties);
    OrganizationVariableResolver orgResolver =
        new OrganizationVariableResolver(clientProperties, organizationsCacheService);
    UserVariableResolver userResolver = new UserVariableResolver(iamCacheService);

    return new VariableEnricher(List.of(configResolver, orgResolver, userResolver));
  }

  private void stubConfig() {
    when(clientProperties.getClient()).thenReturn(clientConfig);
    when(clientConfig.getBaseUrl()).thenReturn(CLIENT_BASE_URL);
  }

  private void stubOrg(NotificationRequest request) {
    when(organizationsCacheService.findOrganizations(Set.of(request.organizationId())))
        .thenReturn(Map.of(request.organizationId(), organization(request.organizationId())));
    when(clientProperties.getAssets()).thenReturn(assetsConfig);
    when(assetsConfig.getFallbackLogoUrl()).thenReturn(FALLBACK_LOGO_URL);
  }

  private void stubUser(NotificationRequest request) {
    when(iamCacheService.findUsers(Set.of(request.subjectUserId())))
        .thenReturn(Map.of(request.subjectUserId(), userDetails()));
  }

  @Nested
  @DisplayName("enrich → sealed resolver variables")
  class SealedVariables {

    @Test
    @DisplayName("should include all resolved system variables in result")
    void shouldIncludeAllResolvedVariables() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();
      stubConfig();
      stubOrg(request);
      stubUser(request);

      Map<String, Serializable> result = enricher().enrich(request, List.of());

      assertThat(result)
          .containsKey("currentYear")
          .containsKey("loginUrl")
          .containsKey("supportUrl")
          .containsKey("organizationName")
          .containsKey("organizationAddress")
          .containsKey("logoUrl")
          .containsKey("adminUsername");
    }

    @Test
    @DisplayName("should omit adminUsername when user not found")
    void shouldOmitAdminUsernameWhenUserNotFound() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();
      stubConfig();
      stubOrg(request);
      when(iamCacheService.findUsers(Set.of(request.subjectUserId()))).thenReturn(Map.of());

      Map<String, Serializable> result = enricher().enrich(request, List.of());

      assertThat(result).doesNotContainKey("adminUsername");
    }
  }

  @Nested
  @DisplayName("enrich → caller variable filtering")
  class CallerVariableFiltering {

    @Test
    @DisplayName("should accept caller variable declared in template definitions")
    void shouldAcceptDeclaredCallerVariable() {
      NotificationRequest request =
          NotificationRequestFixtures.organizationWelcome(
              Map.of("verificationLink", "https://verify.ebikes.test/token"));
      stubConfig();
      stubOrg(request);
      stubUser(request);

      List<TemplateVariable> definitions =
          List.of(
              new TemplateVariable(
                  "verificationLink", VariableType.STRING, "Verification URL", true, true));

      Map<String, Serializable> result = enricher().enrich(request, definitions);

      assertThat(result).containsEntry("verificationLink", "https://verify.ebikes.test/token");
    }

    @Test
    @DisplayName("should reject caller variable not declared in template definitions")
    void shouldRejectUndeclaredCallerVariable() {
      NotificationRequest request =
          NotificationRequestFixtures.organizationWelcome(Map.of("undeclaredKey", "someValue"));
      stubConfig();
      stubOrg(request);
      stubUser(request);

      Map<String, Serializable> result = enricher().enrich(request, List.of());

      assertThat(result).doesNotContainKey("undeclaredKey");
    }

    @Test
    @DisplayName("should reject caller variable that collides with sealed resolver variable")
    void shouldRejectCallerVariableCollidingWithSealedKey() {
      NotificationRequest request =
          NotificationRequestFixtures.organizationWelcome(
              Map.of("loginUrl", "https://attacker.example.com"));
      stubConfig();
      stubOrg(request);
      stubUser(request);

      List<TemplateVariable> definitions =
          List.of(new TemplateVariable("loginUrl", VariableType.STRING, "Login URL", true, false));

      Map<String, Serializable> result = enricher().enrich(request, definitions);

      assertThat(result).containsEntry("loginUrl", CLIENT_BASE_URL);
    }

    @Test
    @DisplayName("should return only sealed variables when caller variables are null")
    void shouldHandleNullCallerVariables() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();
      stubConfig();
      stubOrg(request);
      stubUser(request);

      Map<String, Serializable> result = enricher().enrich(request, List.of());

      assertThat(result).containsKey("currentYear").doesNotContainKey("verificationLink");
    }
  }

  private Organization organization(String id) {
    return new Organization(id, "123 Main St", "eBikes Ltd", null);
  }

  private UserDetails userDetails() {
    return new UserDetails("internal-id", "keycloak-id", "Jane", "Doe", "janedoe");
  }
}
