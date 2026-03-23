package com.ebikes.notifications.database.specifications;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.notifications.dtos.requests.filters.ChannelPreferenceFilter;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.support.database.FilterUtilities;

public final class ChannelPreferenceSpecifications {

  private ChannelPreferenceSpecifications() {
    // prevent instantiation
  }

  public static <T> void buildSharedPredicates(
      List<Predicate> predicates,
      Root<T> root,
      CriteriaQuery<?> query,
      CriteriaBuilder criteriaBuilder,
      ChannelPreferenceFilter filter) {

    FilterUtilities.addIfPresent(
        predicates,
        root,
        query,
        criteriaBuilder,
        filter.getCategory(),
        hasCategory(filter.getCategory()));

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
        filter.getEnabled(),
        hasEnabled(filter.getEnabled()));
  }

  public static <T> Specification<T> hasCategory(NotificationCategory category) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category"), category);
  }

  public static <T> Specification<T> hasChannel(ChannelType channel) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("channel"), channel);
  }

  public static <T> Specification<T> hasEnabled(Boolean enabled) {
    return (root, query, criteriaBuilder) ->
        Boolean.TRUE.equals(enabled)
            ? criteriaBuilder.isTrue(root.get("enabled"))
            : criteriaBuilder.isFalse(root.get("enabled"));
  }
}
