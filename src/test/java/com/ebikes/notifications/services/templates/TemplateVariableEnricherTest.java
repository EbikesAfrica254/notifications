package com.ebikes.notifications.services.templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.time.Year;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import com.ebikes.notifications.dtos.adapters.iam.UserDetails;
import com.ebikes.notifications.dtos.adapters.organizations.Organization;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.services.cache.IamCacheService;
import com.ebikes.notifications.services.cache.OrganizationsCacheService;

@DisplayName("TemplateVariableEnricher")
@ExtendWith(MockitoExtension.class)
class TemplateVariableEnricherTest {

  private static final String ORGANIZATION_ID = "organization-123";
  private static final String SUBJECT_USER_ID = UUID.randomUUID().toString();
  private static final String BASE_URL = "https://client.ebikes.test";
  private static final String OPS_URL = "https://ops.ebikes.test";
  private static final String SUPPORT_URL = "https://support.ebikes.test";

  @Mock private ClientProperties clientProperties;
  @Mock private ClientConfiguration clientConfig;
  @Mock private IamCacheService iamCacheService;
  @Mock private OpsConfiguration opsConfig;
  @Mock private OrganizationsCacheService organizationsCacheService;

  private TemplateVariableEnricher enricher;

  @BeforeEach
  void setUp() {
    when(clientProperties.getClient()).thenReturn(clientConfig);
    when(clientProperties.getOps()).thenReturn(opsConfig);
    when(clientConfig.getBaseUrl()).thenReturn(BASE_URL);
    when(clientConfig.getSupportUrl()).thenReturn(SUPPORT_URL);
    when(opsConfig.getBaseUrl()).thenReturn(OPS_URL);

    enricher =
        new TemplateVariableEnricher(clientProperties, iamCacheService, organizationsCacheService);
  }

  @Nested
  @DisplayName("enrich — system variables")
  class SystemVariables {

    @Test
    @DisplayName("should include currentYear, loginUrl, signupLink, supportUrl")
    void shouldIncludeSystemVariables() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      when(iamCacheService.findUsers(Set.of(SUBJECT_USER_ID)))
          .thenReturn(Map.of(SUBJECT_USER_ID, userDetails()));

      Map<String, Serializable> result = enricher.enrich(request(null));

      assertThat(result)
          .containsEntry("currentYear", String.valueOf(Year.now().getValue()))
          .containsEntry("loginUrl", OPS_URL)
          .containsEntry("signupLink", BASE_URL + "/signup")
          .containsEntry("supportUrl", SUPPORT_URL);
    }

    @Test
    @DisplayName("should merge publisher variables over system variables")
    void shouldMergePublisherVariables() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      when(iamCacheService.findUsers(Set.of(SUBJECT_USER_ID)))
          .thenReturn(Map.of(SUBJECT_USER_ID, userDetails()));

      Map<String, Serializable> result = enricher.enrich(request(Map.of("username", "Alice")));

      assertThat(result).containsEntry("username", "Alice");
    }

    @Test
    @DisplayName("should return only system variables when publisher variables are null")
    void shouldHandleNullPublisherVariables() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      when(iamCacheService.findUsers(Set.of(SUBJECT_USER_ID)))
          .thenReturn(Map.of(SUBJECT_USER_ID, userDetails()));

      Map<String, Serializable> result = enricher.enrich(request(null));

      assertThat(result).containsKey("loginUrl").doesNotContainKey("username");
    }

    @Test
    @DisplayName("publisher variables should override system variables with the same key")
    void shouldAllowPublisherToOverrideSystemVariables() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      when(iamCacheService.findUsers(Set.of(SUBJECT_USER_ID)))
          .thenReturn(Map.of(SUBJECT_USER_ID, userDetails()));

      Map<String, Serializable> result =
          enricher.enrich(request(Map.of("loginUrl", "https://custom.login")));

      assertThat(result).containsEntry("loginUrl", "https://custom.login");
    }
  }

  @Nested
  @DisplayName("enrich — organization variables")
  class OrganizationVariables {

    @Test
    @DisplayName("should include organizationName, logoUrl, organizationAddress when found")
    void shouldIncludeOrgVariables() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      when(iamCacheService.findUsers(Set.of(SUBJECT_USER_ID)))
          .thenReturn(Map.of(SUBJECT_USER_ID, userDetails()));

      Map<String, Serializable> result = enricher.enrich(request(null));

      assertThat(result)
          .containsEntry("organizationName", "eBikes Ltd")
          .containsEntry("logoUrl", "https://logo.url")
          .containsEntry("organizationAddress", "123 Main St");
    }

    @Test
    @DisplayName("should throw ExternalServiceException when organization not found")
    void shouldThrowWhenOrgNotFound() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of());

      NotificationRequest request = request(null);
      assertThatThrownBy(() -> enricher.enrich(request))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should skip organization variable resolution when organizationId is null")
    void shouldSkipWhenOrganizationIdIsNull() {
      when(iamCacheService.findUsers(Set.of(SUBJECT_USER_ID)))
          .thenReturn(Map.of(SUBJECT_USER_ID, userDetails()));

      Map<String, Serializable> result = enricher.enrich(requestWithOrganizationId());

      assertThat(result).containsKey("loginUrl").doesNotContainKey("organizationName");
      verify(organizationsCacheService, never()).findOrganizations(any());
    }
  }

  @Nested
  @DisplayName("enrich — user variables")
  class UserVariables {

    @Test
    @DisplayName("should include adminUsername when subjectUserId resolves to a user")
    void shouldIncludeAdminUsername() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      when(iamCacheService.findUsers(Set.of(SUBJECT_USER_ID)))
          .thenReturn(Map.of(SUBJECT_USER_ID, userDetails()));

      Map<String, Serializable> result = enricher.enrich(request(null));

      assertThat(result).containsEntry("adminUsername", "Jane Doe");
    }

    @Test
    @DisplayName("should fall back to username when firstName or lastName is null")
    void shouldFallBackToUsername() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      when(iamCacheService.findUsers(Set.of(SUBJECT_USER_ID)))
          .thenReturn(
              Map.of(SUBJECT_USER_ID, new UserDetails(SUBJECT_USER_ID, null, null, "janedoe")));

      Map<String, Serializable> result = enricher.enrich(request(null));

      assertThat(result).containsEntry("adminUsername", "janedoe");
    }

    @Test
    @DisplayName("should skip user variable resolution when subjectUserId is null")
    void shouldSkipWhenSubjectUserIdIsNull() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));

      Map<String, Serializable> result = enricher.enrich(requestWithSubjectUserId());

      assertThat(result).doesNotContainKey("adminUsername");
      verify(iamCacheService, never()).findUsers(any());
    }

    @Test
    @DisplayName("should skip adminUsername when user not found in cache or adapter")
    void shouldSkipWhenUserNotFound() {
      when(organizationsCacheService.findOrganizations(Set.of(ORGANIZATION_ID)))
          .thenReturn(Map.of(ORGANIZATION_ID, organization()));
      when(iamCacheService.findUsers(Set.of(SUBJECT_USER_ID))).thenReturn(Map.of());

      Map<String, Serializable> result = enricher.enrich(request(null));

      assertThat(result).doesNotContainKey("adminUsername");
    }
  }

  private NotificationRequest request(Map<String, Serializable> variables) {
    return new NotificationRequest(
        null,
        NotificationCategory.OPERATIONAL,
        ChannelType.EMAIL,
        "ORGANIZATION_WELCOME",
        ORGANIZATION_ID,
        "recipient@ebikes.test",
        "test-reference",
        SUBJECT_USER_ID,
        "ORGANIZATION_WELCOME",
        null,
        variables);
  }

  private NotificationRequest requestWithOrganizationId() {
    return new NotificationRequest(
        null,
        NotificationCategory.OPERATIONAL,
        ChannelType.EMAIL,
        "ORGANIZATION_WELCOME",
        null,
        "recipient@ebikes.test",
        "test-reference",
        SUBJECT_USER_ID,
        "ORGANIZATION_WELCOME",
        null,
        null);
  }

  private NotificationRequest requestWithSubjectUserId() {
    return new NotificationRequest(
        null,
        NotificationCategory.OPERATIONAL,
        ChannelType.EMAIL,
        "ORGANIZATION_WELCOME",
        ORGANIZATION_ID,
        "recipient@ebikes.test",
        "test-reference",
        null,
        "ORGANIZATION_WELCOME",
        null,
        null);
  }

  private static Organization organization() {
    return new Organization(ORGANIZATION_ID, "123 Main St", "eBikes Ltd", "https://logo.url");
  }

  private static UserDetails userDetails() {
    return new UserDetails(SUBJECT_USER_ID, "Jane", "Doe", "janedoe");
  }
}
