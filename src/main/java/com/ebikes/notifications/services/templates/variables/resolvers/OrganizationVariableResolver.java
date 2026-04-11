package com.ebikes.notifications.services.templates.variables.resolvers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ebikes.notifications.configurations.properties.ClientProperties;
import com.ebikes.notifications.dtos.adapters.organizations.Organization;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.services.cache.OrganizationsCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationVariableResolver implements VariableResolver {

  private static final String LOGO_URL = "logoUrl";
  private static final String ORGANIZATION_ADDRESS = "organizationAddress";
  private static final String ORGANIZATION_NAME = "organizationName";

  private final ClientProperties clientProperties;
  private final OrganizationsCacheService organizationsCacheService;

  @Override
  public Map<String, Serializable> resolve(NotificationRequest request) {
    if (!hasText(request.organizationId())) {
      log.warn("No organizationId on request - skipping organization variable resolution");
      return Map.of();
    }

    Map<String, Organization> result =
        organizationsCacheService.findOrganizations(Set.of(request.organizationId()));

    Organization organization = result.get(request.organizationId());

    if (organization == null) {
      throw new ExternalServiceException(
          "organizations-service",
          "Organization not found for id=" + request.organizationId(),
          ResponseCode.EXTERNAL_SERVICE_ERROR);
    }

    Map<String, Serializable> variables = new HashMap<>();

    variables.put(ORGANIZATION_ADDRESS, organization.address());
    variables.put(ORGANIZATION_NAME, organization.displayName());

    String logoUrl = resolveLogoUrl(organization);
    if (logoUrl != null) {
      variables.put(LOGO_URL, logoUrl);
    }

    log.debug(
        "Resolved organization variables: organizationId={} keys={}",
        organization.id(),
        variables.keySet());

    return variables;
  }

  private String resolveLogoUrl(Organization organization) {
    if (hasText(organization.logoUrl())) {
      return organization.logoUrl();
    }

    String fallback = clientProperties.getAssets().getFallbackLogoUrl();
    if (hasText(fallback)) {
      log.debug(
          "Organization has no logoUrl - using configured fallback: organizationId={}",
          organization.id());
      return fallback;
    }

    log.warn(
        "Organization has no logoUrl and no fallback is configured: organizationId={}",
        organization.id());
    return null;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
