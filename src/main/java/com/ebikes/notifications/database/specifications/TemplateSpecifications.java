package com.ebikes.notifications.database.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.dtos.requests.filters.TemplateFilter;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.TemplateContentType;
import com.ebikes.notifications.support.database.FilterUtilities;

public final class TemplateSpecifications {

  public static final String FIELD_CHANNEL = "channel";
  public static final String FIELD_CONTENT_TYPE = "templateContentType";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_IS_ACTIVE = "isActive";
  public static final String FIELD_NAME = "name";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(FIELD_CHANNEL, FIELD_CONTENT_TYPE, FIELD_CREATED_AT, FIELD_IS_ACTIVE, FIELD_NAME);

  private TemplateSpecifications() {
    // prevent instantiation
  }

  public static Specification<Template> buildSpecification(TemplateFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      FilterUtilities.addIfPresent(
          predicates, root, query, criteriaBuilder, filter.getName(), hasName(filter.getName()));

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
          filter.getTemplateContentType(),
          hasTemplateContentType(filter.getTemplateContentType()));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getIsActive(),
          isActive(filter.getIsActive()));

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Template> hasChannel(ChannelType channel) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_CHANNEL), channel);
  }

  public static Specification<Template> hasTemplateContentType(
      TemplateContentType templateContentType) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_CONTENT_TYPE), templateContentType);
  }

  public static Specification<Template> hasName(String name) {
    return FilterUtilities.likeIgnoreCase(FIELD_NAME, name);
  }

  public static Specification<Template> isActive(Boolean active) {
    return (root, query, criteriaBuilder) ->
        Boolean.TRUE.equals(active)
            ? criteriaBuilder.isTrue(root.get(FIELD_IS_ACTIVE))
            : criteriaBuilder.isFalse(root.get(FIELD_IS_ACTIVE));
  }
}
