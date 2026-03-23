package com.ebikes.notifications.support.security;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ebikes.notifications.enums.UserRole;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RBACUtilities {

  public static boolean hasSystemAdminRole(Set<UserRole> roles) {
    return roles.contains(UserRole.SYSTEM_ADMIN);
  }

  public static boolean isBranchRole(UserRole role) {
    return role.name().startsWith("BRANCH_") || role == UserRole.AGENT;
  }

  public static boolean isOrganizationRole(UserRole role) {
    return role.name().startsWith("ORGANIZATION_") || role == UserRole.SYSTEM_ADMIN;
  }

  public static Set<UserRole> parseRoles(Set<String> roleNames) {
    return roleNames.stream()
        .map(UserRole::fromString)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}
