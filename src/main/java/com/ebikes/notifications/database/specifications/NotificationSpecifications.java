package com.ebikes.notifications.database.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.requests.filters.NotificationFilter;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationStatus;
import com.ebikes.notifications.support.database.FilterUtilities;

public final class NotificationSpecifications {

  public static final String FIELD_BRANCH_ID = "branchId";
  public static final String FIELD_CHANNEL = "channel";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_ORGANIZATION_ID = "organizationId";
  public static final String FIELD_RECIPIENT = "recipient";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_TEMPLATE = "template";
  public static final String FIELD_TEMPLATE_ID = "id";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(FIELD_BRANCH_ID, FIELD_CHANNEL, FIELD_CREATED_AT, FIELD_ORGANIZATION_ID, FIELD_STATUS);

  private NotificationSpecifications() {
    // prevent instantiation
  }

  public static Specification<Notification> buildSpecification(NotificationFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(
          AuthorizationSpecifications.forNotifications().toPredicate(root, query, criteriaBuilder));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getBranchId(),
          hasBranchId(filter.getBranchId()));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getOrganizationId(),
          hasOrganizationId(filter.getOrganizationId()));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getRecipient(),
          hasRecipient(filter.getRecipient()));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getTemplateId(),
          hasTemplateId(filter.getTemplateId()));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getChannel(),
          hasChannel(filter.getChannel()));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getStatus(),
          hasStatus(filter.getStatus()));

      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_CREATED_AT,
          filter.getCreatedAtFrom(),
          filter.getCreatedAtTo());

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Notification> hasBranchId(String branchId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_BRANCH_ID), branchId);
  }

  public static Specification<Notification> hasChannel(ChannelType channel) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_CHANNEL), channel);
  }

  public static Specification<Notification> hasOrganizationId(String organizationId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_ORGANIZATION_ID), organizationId);
  }

  public static Specification<Notification> hasRecipient(String recipient) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_RECIPIENT), recipient.trim());
  }

  public static Specification<Notification> hasStatus(NotificationStatus status) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(FIELD_STATUS), status);
  }

  public static Specification<Notification> hasTemplateId(UUID templateId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_TEMPLATE).get(FIELD_TEMPLATE_ID), templateId);
  }
}
