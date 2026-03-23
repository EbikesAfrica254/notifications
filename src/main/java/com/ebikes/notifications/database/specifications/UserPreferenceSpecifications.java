package com.ebikes.notifications.database.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.notifications.database.entities.UserChannelPreference;
import com.ebikes.notifications.dtos.requests.filters.ChannelPreferenceFilter;

public final class UserPreferenceSpecifications {

  public static final String FIELD_CATEGORY = "category";
  public static final String FIELD_CHANNEL = "channel";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_ENABLED = "enabled";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(FIELD_CATEGORY, FIELD_CHANNEL, FIELD_CREATED_AT, FIELD_ENABLED);

  private UserPreferenceSpecifications() {
    // prevent instantiation
  }

  public static Specification<UserChannelPreference> buildSpecification(
      ChannelPreferenceFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(
          AuthorizationSpecifications.forUserPreferences()
              .toPredicate(root, query, criteriaBuilder));

      ChannelPreferenceSpecifications.buildSharedPredicates(
          predicates, root, query, criteriaBuilder, filter);

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
