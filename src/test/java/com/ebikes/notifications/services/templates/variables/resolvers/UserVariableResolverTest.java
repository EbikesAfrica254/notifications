package com.ebikes.notifications.services.templates.variables.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.notifications.dtos.adapters.iam.UserDetails;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.services.cache.IamCacheService;
import com.ebikes.notifications.support.fixtures.NotificationRequestFixtures;

@DisplayName("UserVariableResolver")
@ExtendWith(MockitoExtension.class)
class UserVariableResolverTest {

  private static final String INTERNAL_ID = "internal-id";
  private static final String KEYCLOAK_ID = "keycloak-id";
  private static final String FIRST_NAME = "Jane";
  private static final String LAST_NAME = "Doe";
  private static final String USERNAME = "janedoe";
  private static final String FULL_NAME = FIRST_NAME + " " + LAST_NAME;
  private static final String ADMIN_USERNAME = "adminUsername";

  @Mock private IamCacheService iamCacheService;

  @InjectMocks private UserVariableResolver resolver;

  @Nested
  @DisplayName("resolve → skips when subjectUserId absent")
  class SkipResolution {

    @Test
    @DisplayName("should return empty map when subjectUserId is null")
    void shouldReturnEmptyWhenSubjectUserIdNull() {
      NotificationRequest request =
          NotificationRequestFixtures.organizationWelcomeWithoutSubjectUserId();

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("resolve → user not found")
  class UserNotFound {

    @Test
    @DisplayName("should return empty map when user not found in cache or adapter")
    void shouldReturnEmptyWhenUserNotFound() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();

      when(iamCacheService.findUsers(Set.of(request.subjectUserId()))).thenReturn(Map.of());

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("resolve → adminUsername")
  class AdminUsername {

    @Test
    @DisplayName("should resolve adminUsername as firstName and lastName when both present")
    void shouldResolveFullName() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();

      when(iamCacheService.findUsers(Set.of(request.subjectUserId())))
          .thenReturn(Map.of(request.subjectUserId(), userDetails(FIRST_NAME, LAST_NAME)));

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).containsEntry(ADMIN_USERNAME, FULL_NAME);
    }

    @Test
    @DisplayName("should fall back to username when firstName is null")
    void shouldFallBackToUsernameWhenFirstNameNull() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();

      when(iamCacheService.findUsers(Set.of(request.subjectUserId())))
          .thenReturn(Map.of(request.subjectUserId(), userDetails(null, LAST_NAME)));

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).containsEntry(ADMIN_USERNAME, USERNAME);
    }

    @Test
    @DisplayName("should fall back to username when lastName is null")
    void shouldFallBackToUsernameWhenLastNameNull() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();

      when(iamCacheService.findUsers(Set.of(request.subjectUserId())))
          .thenReturn(Map.of(request.subjectUserId(), userDetails(FIRST_NAME, null)));

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).containsEntry(ADMIN_USERNAME, USERNAME);
    }

    @Test
    @DisplayName("should fall back to username when both firstName and lastName are null")
    void shouldFallBackToUsernameWhenBothNull() {
      NotificationRequest request = NotificationRequestFixtures.organizationWelcome();

      when(iamCacheService.findUsers(Set.of(request.subjectUserId())))
          .thenReturn(Map.of(request.subjectUserId(), userDetails(null, null)));

      Map<String, Serializable> result = resolver.resolve(request);

      assertThat(result).containsEntry(ADMIN_USERNAME, USERNAME);
    }
  }

  private UserDetails userDetails(String firstName, String lastName) {
    return new UserDetails(INTERNAL_ID, KEYCLOAK_ID, firstName, lastName, USERNAME);
  }
}
