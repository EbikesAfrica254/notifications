package com.ebikes.notifications.services.templates.variables.resolvers;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ebikes.notifications.dtos.adapters.iam.UserDetails;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.services.cache.IamCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserVariableResolver implements VariableResolver {

  private static final String ADMIN_USERNAME = "adminUsername";

  private final IamCacheService iamCacheService;

  @Override
  public Map<String, Serializable> resolve(NotificationRequest request) {
    if (!hasText(request.subjectUserId())) {
      log.debug("No subjectUserId on request - skipping user variable resolution");
      return Map.of();
    }

    Map<String, UserDetails> result = iamCacheService.findUsers(Set.of(request.subjectUserId()));
    UserDetails user = result.get(request.subjectUserId());

    if (user == null) {
      log.warn(
          "User not found - skipping user variable resolution: subjectUserId={}",
          request.subjectUserId());
      return Map.of();
    }

    log.debug(
        "Resolved user variables: subjectUserId={} keys=[{}]",
        request.subjectUserId(),
        ADMIN_USERNAME);

    return Map.of(ADMIN_USERNAME, user.displayName());
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
