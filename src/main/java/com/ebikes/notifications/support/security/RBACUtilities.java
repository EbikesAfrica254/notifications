package com.ebikes.notifications.support.security;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ebikes.notifications.enums.UserRole;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RBACUtilities {

  private static final Set<UserRole> ADMIN_ROLES =
      Set.of(UserRole.SYSTEM_ADMIN, UserRole.ORGANIZATION_ADMIN, UserRole.BRANCH_ADMIN);

  public static int getHighestAuthority(Set<UserRole> roles) {
    return roles.stream().mapToInt(RBACUtilities::getRoleAuthority).max().orElse(0);
  }

  public static int getRequiredAuthorityToCreate(UserRole targetRole) {
    return switch (targetRole) {
      case SYSTEM_ADMIN -> 100;
      case ORGANIZATION_ADMIN -> 80;
      case BRANCH_ADMIN,
          ORGANIZATION_MAKER,
          ORGANIZATION_OPERATOR,
          ORGANIZATION_CHECKER,
          ORGANIZATION_FLEET_MANAGER,
          ORGANIZATION_FLEET_SUPPORT,
          ORGANIZATION_INVENTORY_MANAGER ->
          60;
      case BRANCH_OPERATOR,
          BRANCH_CHECKER,
          BRANCH_MAKER,
          BRANCH_FLEET_MANAGER,
          BRANCH_FLEET_SUPPORT,
          BRANCH_INVENTORY_MANAGER,
          AGENT,
          CUSTOMER ->
          40;
    };
  }

  public static boolean hasAdminRole(Set<UserRole> roles) {
    return roles.stream().anyMatch(ADMIN_ROLES::contains);
  }

  public static boolean hasAdminRoleFromNames(Set<String> roleNames) {
    return hasAdminRole(parseRoles(roleNames));
  }

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

  private static int getRoleAuthority(UserRole role) {
    return switch (role) {
      case SYSTEM_ADMIN -> 100;
      case ORGANIZATION_ADMIN -> 80;
      case BRANCH_ADMIN -> 60;
      case ORGANIZATION_OPERATOR,
          ORGANIZATION_CHECKER,
          ORGANIZATION_MAKER,
          ORGANIZATION_FLEET_MANAGER,
          ORGANIZATION_FLEET_SUPPORT,
          ORGANIZATION_INVENTORY_MANAGER ->
          40;
      case BRANCH_OPERATOR,
          BRANCH_CHECKER,
          BRANCH_MAKER,
          BRANCH_FLEET_MANAGER,
          BRANCH_FLEET_SUPPORT,
          BRANCH_INVENTORY_MANAGER,
          AGENT ->
          20;
      case CUSTOMER -> 0;
    };
  }
}
