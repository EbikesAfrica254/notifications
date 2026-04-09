package com.ebikes.notifications.services.templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.time.Year;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.notifications.configurations.properties.ClientProperties;
import com.ebikes.notifications.configurations.properties.ClientProperties.ClientConfiguration;
import com.ebikes.notifications.configurations.properties.ClientProperties.OpsConfiguration;
import com.ebikes.notifications.dtos.adapters.organizations.Organization;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.services.cache.OrganizationCacheService;

@DisplayName("TemplateVariableEnricher")
@ExtendWith(MockitoExtension.class)
class TemplateVariableEnricherTest {

  private static final String ORGANIZATION_ID = "organization-123";
  private static final String BASE_URL = "https://client.ebikes.test";
  private static final String OPS_URL = "https://ops.ebikes.test";
  private static final String SUPPORT_URL = "https://support.ebikes.test";

  @Mock private ClientProperties clientProperties;
  @Mock private ClientConfiguration clientConfig;
  @Mock private OpsConfiguration opsConfig;
  @Mock private OrganizationCacheService organizationCacheService;

  private TemplateVariableEnricher enricher;

  @BeforeEach
  void setUp() {
    when(clientProperties.getClient()).thenReturn(clientConfig);
    when(clientProperties.getOps()).thenReturn(opsConfig);
    when(clientConfig.getBaseUrl()).thenReturn(BASE_URL);
    when(clientConfig.getSupportUrl()).thenReturn(SUPPORT_URL);
    when(opsConfig.getBaseUrl()).thenReturn(OPS_URL);

    enricher = new TemplateVariableEnricher(clientProperties, organizationCacheService);
  }

  @Nested
  @DisplayName("enrich — system variables")
  class SystemVariables {

    @Test
    @DisplayName("should include currentYear, loginUrl, signupLink, supportUrl")
    void shouldIncludeSystemVariables() {
      when(organizationCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));

      Map<String, Serializable> result = enricher.enrich(ORGANIZATION_ID, null);

      assertThat(result)
          .containsEntry("currentYear", String.valueOf(Year.now().getValue()))
          .containsEntry("loginUrl", OPS_URL)
          .containsEntry("signupLink", BASE_URL + "/signup")
          .containsEntry("supportUrl", SUPPORT_URL);
    }

    @Test
    @DisplayName("should merge publisher variables over system variables")
    void shouldMergePublisherVariables() {
      when(organizationCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      Map<String, Serializable> publisherVars = Map.of("username", "Alice");

      Map<String, Serializable> result = enricher.enrich(ORGANIZATION_ID, publisherVars);

      assertThat(result).containsEntry("username", "Alice");
    }

    @Test
    @DisplayName("should return only system variables when publisher variables are null")
    void shouldHandleNullPublisherVariables() {
      when(organizationCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));

      Map<String, Serializable> result = enricher.enrich(ORGANIZATION_ID, null);

      assertThat(result).containsKey("loginUrl");
      assertThat(result).doesNotContainKey("username");
    }

    @Test
    @DisplayName("publisher variables should override system variables with the same key")
    void shouldAllowPublisherToOverrideSystemVariables() {
      when(organizationCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      Map<String, Serializable> publisherVars = Map.of("loginUrl", "https://custom.login");

      Map<String, Serializable> result = enricher.enrich(ORGANIZATION_ID, publisherVars);

      assertThat(result).containsEntry("loginUrl", "https://custom.login");
    }
  }

  @Nested
  @DisplayName("enrich — organization variables")
  class OrganizationVariables {

    @Test
    @DisplayName(
        "should include organizationName, logoUrl, organizationAddress when organization found")
    void shouldIncludeOrgVariables() {
      Organization organization = organization();
      when(organizationCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization));

      Map<String, Serializable> result = enricher.enrich(ORGANIZATION_ID, null);

      assertThat(result)
          .containsEntry("organizationName", "eBikes Ltd")
          .containsEntry("logoUrl", "https://logo.url")
          .containsEntry("organizationAddress", "123 Main St");
    }

    @Test
    @DisplayName(
        "should throw ExternalServiceException when organization not found in cache or adapter")
    void shouldThrowWhenOrgNotFound() {
      when(organizationCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of());

      assertThatThrownBy(() -> enricher.enrich(ORGANIZATION_ID, null))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should skip organization variable resolution when organizationId is null")
    void shouldSkipWhenOrganizationIdIsNull() {
      Map<String, Serializable> result = enricher.enrich(null, null);

      assertThat(result).containsKey("loginUrl");
      assertThat(result).doesNotContainKey("organizationName");
    }

    @Test
    @DisplayName("should skip organization variable resolution when organizationId is blank")
    void shouldSkipWhenOrganizationIdIsBlank() {
      Map<String, Serializable> result = enricher.enrich("  ", null);

      assertThat(result).containsKey("loginUrl");
      assertThat(result).doesNotContainKey("organizationName");
    }
  }

  private static Organization organization() {
    return new Organization(ORGANIZATION_ID, "123 Main St", "eBikes Ltd", "https://logo.url");
  }
}
