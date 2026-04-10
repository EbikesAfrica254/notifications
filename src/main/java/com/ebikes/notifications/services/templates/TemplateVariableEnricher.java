package com.ebikes.notifications.services.templates;

import java.io.Serializable;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ebikes.notifications.configurations.properties.ClientProperties;
import com.ebikes.notifications.dtos.adapters.iam.UserDetails;
import com.ebikes.notifications.dtos.adapters.organizations.Organization;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.services.cache.IamCacheService;
import com.ebikes.notifications.services.cache.OrganizationsCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateVariableEnricher {

  private static final String ADMIN_USERNAME = "adminUsername";
  private static final String CURRENT_YEAR = "currentYear";
  private static final String LOGIN_URL = "loginUrl";
  private static final String LOGO_URL = "logoUrl";
  private static final String ORGANIZATION_ADDRESS = "organizationAddress";
  private static final String ORGANIZATION_NAME = "organizationName";
  private static final String SIGNUP_LINK = "signupLink";
  private static final String SUPPORT_URL = "supportUrl";
  private static final String SIGNUP_PATH = "/signup";

  private final ClientProperties clientProperties;
  private final IamCacheService iamCacheService;
  private final OrganizationsCacheService organizationsCacheService;

  public Map<String, Serializable> enrich(NotificationRequest request) {
    Map<String, Serializable> enriched = new HashMap<>(resolveSystemVariables(request));
    if (request.variables() != null) {
      enriched.putAll(request.variables());
    }
    return Map.copyOf(enriched);
  }

  private Map<String, Serializable> resolveSystemVariables(NotificationRequest request) {
    Map<String, Serializable> system = new HashMap<>();
    system.put(CURRENT_YEAR, String.valueOf(Year.now().getValue()));
    system.put(LOGIN_URL, clientProperties.getOps().getBaseUrl());
    system.put(SIGNUP_LINK, clientProperties.getClient().getBaseUrl() + SIGNUP_PATH);
    system.put(SUPPORT_URL, clientProperties.getClient().getSupportUrl());
    resolveOrganizationVariables(request.organizationId(), system);
    resolveUserVariables(request.subjectUserId(), system);
    return system;
  }

  private void resolveOrganizationVariables(
      String organizationId, Map<String, Serializable> target) {
    if (organizationId == null || organizationId.isBlank()) {
      log.warn("No organizationId provided — skipping organization variable resolution");
      return;
    }

    Map<String, Organization> organizations =
        organizationsCacheService.findOrganizations(Set.of(organizationId));
    Organization organization = organizations.get(organizationId);

    if (organization == null) {
      log.warn(
          "Organization not found in cache or adapter — skipping organization variables:"
              + " organizationId={}",
          organizationId);
      throw new ExternalServiceException(
          "organizations-service",
          "Organization not found for id=" + organizationId,
          ResponseCode.EXTERNAL_SERVICE_ERROR);
    }

    target.put(ORGANIZATION_NAME, organization.displayName());
    target.put(LOGO_URL, organization.logoUrl());
    target.put(ORGANIZATION_ADDRESS, organization.address());
  }

  private void resolveUserVariables(String subjectUserId, Map<String, Serializable> target) {
    if (subjectUserId == null || subjectUserId.isBlank()) {
      log.warn("No subjectUserId provided — skipping user variable resolution");
      return;
    }

    Map<String, UserDetails> users = iamCacheService.findUsers(Set.of(subjectUserId));
    UserDetails user = users.get(subjectUserId);

    if (user == null) {
      log.warn(
          "User not found in cache or adapter — skipping user variables: subjectUserId={}",
          subjectUserId);
      return;
    }

    target.put(ADMIN_USERNAME, user.displayName());
  }
}
