package com.ebikes.notifications.database.specifications;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.entities.OrganizationChannelPreference;
import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.database.entities.UserChannelPreference;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.enums.UserRole;
import com.ebikes.notifications.exceptions.AuthorizationException;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.security.RBACUtilities;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthorizationSpecifications {

  private static final String NOTIFICATION_ASSOCIATION = "notification";
  private static final String RECIPIENT = "recipient";

  private AuthorizationSpecifications() {
    // prevent instantiation
  }

  public static Specification<Delivery> forDeliveries() {
    return resolveOrganizationScope(
        "deliveries",
        AuthorizationSpecifications::filterByOrganizationId,
        AuthorizationSpecifications::filterByNotificationBranchId,
        AuthorizationSpecifications::filterByNotificationRecipient);
  }

  public static Specification<Notification> forNotifications() {
    return resolveOrganizationScope(
        "notifications",
        AuthorizationSpecifications::filterByOrganizationId,
        AuthorizationSpecifications::filterByBranchId,
        AuthorizationSpecifications::filterByRecipient);
  }

  public static Specification<OrganizationChannelPreference> forOrganizationPreferences() {
    Set<UserRole> roles = RBACUtilities.parseRoles(ExecutionContext.getRoles());

    if (RBACUtilities.hasSystemAdminRole(roles)) {
      log.debug("SYSTEM_ADMIN access: returning all organization preferences without filtering");
      return noFilter();
    }

    String activeOrganization = validateActiveOrganization("organization preferences");
    return filterByOrganizationId(activeOrganization);
  }

  public static Specification<Template> forTemplates() {
    Set<UserRole> roles = RBACUtilities.parseRoles(ExecutionContext.getRoles());

    if (!RBACUtilities.hasSystemAdminRole(roles)) {
      throw new AuthorizationException(ResponseCode.FORBIDDEN, "Unauthorized access");
    }

    return noFilter();
  }

  public static Specification<UserChannelPreference> forUserPreferences() {
    Set<UserRole> roles = RBACUtilities.parseRoles(ExecutionContext.getRoles());

    if (RBACUtilities.hasSystemAdminRole(roles)) {
      log.debug("SYSTEM_ADMIN access: returning all user preferences without filtering");
      return noFilter();
    }

    String activeOrganization = validateActiveOrganization("user preferences");

    if (roles.stream().anyMatch(RBACUtilities::isOrganizationRole)) {
      log.debug(
          "ORGANIZATION-level access: filtering user preferences by organizationId={}",
          activeOrganization);
      return filterByOrganizationId(activeOrganization);
    }

    String currentUserId = validateCurrentUser();
    log.debug("USER-level access: filtering user preferences by userId={}", currentUserId);
    return filterByUserId(currentUserId);
  }

  @SuppressWarnings("unchecked")
  static Join<Delivery, Notification> notificationJoin(Root<Delivery> root) {

    return root.getJoins().stream()
        .filter(j -> j.getAttribute().getName().equals(NOTIFICATION_ASSOCIATION))
        .map(j -> (Join<Delivery, Notification>) j)
        .findFirst()
        .orElseGet(() -> root.join(NOTIFICATION_ASSOCIATION, JoinType.INNER));
  }

  public static Specification<Delivery> withNotificationFetch() {
    return (root, query, criteriaBuilder) -> {
      if (!Long.class.equals(query.getResultType())) {
        boolean alreadyFetched =
            root.getFetches().stream()
                .anyMatch(f -> f.getAttribute().getName().equals(NOTIFICATION_ASSOCIATION));
        if (!alreadyFetched) {
          root.fetch(NOTIFICATION_ASSOCIATION, JoinType.INNER);
        }
      }
      return criteriaBuilder.conjunction();
    };
  }

  private static <T> Specification<T> resolveOrganizationScope(
      String resourceType,
      Function<String, Specification<T>> organizationFilter,
      Function<String, Specification<T>> branchFilter,
      BiFunction<String, String, Specification<T>> recipientFilter) {

    Set<UserRole> roles = RBACUtilities.parseRoles(ExecutionContext.getRoles());

    if (RBACUtilities.hasSystemAdminRole(roles)) {
      log.debug("SYSTEM_ADMIN access: returning all {} without filtering", resourceType);
      return noFilter();
    }

    String activeOrganization = validateActiveOrganization(resourceType);

    if (roles.stream().anyMatch(RBACUtilities::isOrganizationRole)) {
      log.debug("ORGANIZATION-level access: filtering by organizationId={}", activeOrganization);
      return organizationFilter.apply(activeOrganization);
    }

    if (roles.stream().anyMatch(RBACUtilities::isBranchRole)) {
      String activeBranch = validateActiveBranch(resourceType);
      log.debug(
          "BRANCH-level access: filtering by organizationId={} branchId={}",
          activeOrganization,
          activeBranch);
      return Specification.allOf(
          organizationFilter.apply(activeOrganization), branchFilter.apply(activeBranch));
    }

    String userEmail = validateUserEmail();
    String userPhoneNumber = ExecutionContext.getPhoneNumber();

    log.debug("USER-level access: filtering by organizationId={} recipient", activeOrganization);

    return Specification.allOf(
        organizationFilter.apply(activeOrganization),
        recipientFilter.apply(userEmail, userPhoneNumber));
  }

  private static <T> Specification<T> filterByOrganizationId(String organizationId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("organizationId"), organizationId);
  }

  private static <T> Specification<T> filterByBranchId(String branchId) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("branchId"), branchId);
  }

  private static Specification<Delivery> filterByNotificationBranchId(String branchId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(notificationJoin(root).get("branchId"), branchId);
  }

  private static Specification<Delivery> filterByNotificationRecipient(
      String email, String phoneNumber) {
    return (root, query, criteriaBuilder) -> {
      if (email != null && phoneNumber != null) {
        return criteriaBuilder.or(
            criteriaBuilder.equal(notificationJoin(root).get(RECIPIENT), email),
            criteriaBuilder.equal(notificationJoin(root).get(RECIPIENT), phoneNumber));
      } else if (email != null) {
        return criteriaBuilder.equal(notificationJoin(root).get(RECIPIENT), email);
      } else if (phoneNumber != null) {
        return criteriaBuilder.equal(notificationJoin(root).get(RECIPIENT), phoneNumber);
      } else {
        log.warn(
            "No recipient identifiers available for delivery-level user access - returning no"
                + " results");
        return criteriaBuilder.disjunction();
      }
    };
  }

  private static Specification<Notification> filterByRecipient(String email, String phoneNumber) {
    return (root, query, criteriaBuilder) -> {
      if (email != null && phoneNumber != null) {
        return criteriaBuilder.or(
            criteriaBuilder.equal(root.get(RECIPIENT), email),
            criteriaBuilder.equal(root.get(RECIPIENT), phoneNumber));
      } else if (email != null) {
        return criteriaBuilder.equal(root.get(RECIPIENT), email);
      } else if (phoneNumber != null) {
        return criteriaBuilder.equal(root.get(RECIPIENT), phoneNumber);
      } else {
        log.warn(
            "No recipient identifiers available for notification-level user access - returning no"
                + " results");
        return criteriaBuilder.disjunction();
      }
    };
  }

  private static Specification<UserChannelPreference> filterByUserId(String userId) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
  }

  private static <T> Specification<T> noFilter() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
  }

  private static String validateActiveOrganization(String resourceType) {
    String activeOrganization = ExecutionContext.getActiveOrganization();
    if (activeOrganization == null || activeOrganization.isBlank()) {
      log.error("Missing active_organization claim for {} access", resourceType);
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Active organization context required for this operation");
    }
    return activeOrganization;
  }

  private static String validateActiveBranch(String resourceType) {
    String activeBranch = ExecutionContext.getActiveBranch();
    if (activeBranch == null || activeBranch.isBlank()) {
      log.error("Missing active_branch claim for {} access", resourceType);
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Active branch context required for this operation");
    }
    return activeBranch;
  }

  private static String validateCurrentUser() {
    String currentUserId = ExecutionContext.getUserId();
    if (currentUserId == null || currentUserId.isBlank()) {
      log.error("Missing user_id claim");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "User context required for this operation");
    }
    return currentUserId;
  }

  private static String validateUserEmail() {
    String email = ExecutionContext.getEmail();
    if (email == null || email.isBlank()) {
      log.error("Missing email claim");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "User email required for this operation");
    }
    return email;
  }
}
