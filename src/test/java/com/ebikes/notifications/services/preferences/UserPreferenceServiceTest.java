package com.ebikes.notifications.services.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.notifications.database.entities.UserChannelPreference;
import com.ebikes.notifications.database.repositories.UserPreferenceRepository;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.dtos.requests.filters.ChannelPreferenceFilter;
import com.ebikes.notifications.dtos.requests.preferences.user.CreateUserPreferenceRequest;
import com.ebikes.notifications.dtos.requests.preferences.user.UpdateUserPreferenceRequest;
import com.ebikes.notifications.dtos.responses.preferences.user.UserPreferenceResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.enums.UserRole;
import com.ebikes.notifications.exceptions.DuplicateResourceException;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.UserPreferenceMapper;
import com.ebikes.notifications.support.audit.AuditTemplate;
import com.ebikes.notifications.support.audit.ThrowingRunnable;
import com.ebikes.notifications.support.audit.ThrowingSupplier;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.fixtures.SecurityFixtures;
import com.ebikes.notifications.support.fixtures.UserPreferenceFixtures;

@DisplayName("UserPreferenceService")
@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

  private static final String TEST_USER_ID = SecurityFixtures.TEST_USER_ID;
  private static final String TEST_ORGANIZATION_ID = SecurityFixtures.TEST_ORGANIZATION_ID;

  @Mock private AuditTemplate auditTemplate;
  @Mock private OrganizationPreferenceService organizationPreferenceService;
  @Mock private UserPreferenceMapper mapper;
  @Mock private UserPreferenceRepository repository;

  private UserPreferenceService service;

  @BeforeEach
  void setUp() {
    service =
        new UserPreferenceService(auditTemplate, organizationPreferenceService, mapper, repository);
    SecurityFixtures.setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
  }

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
  }

  private UserPreferenceResponse stubResponse(boolean enabled) {
    return new UserPreferenceResponse(
        NotificationCategory.MARKETING,
        ChannelType.EMAIL,
        null,
        enabled,
        UUID.randomUUID(),
        TEST_ORGANIZATION_ID,
        null,
        TEST_USER_ID,
        null);
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should create and return preference response")
    void shouldCreatePreference() {
      CreateUserPreferenceRequest request =
          new CreateUserPreferenceRequest(NotificationCategory.MARKETING, ChannelType.EMAIL, true);
      UserChannelPreference preference =
          UserPreferenceFixtures.enabled(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING);
      UserPreferenceResponse response = stubResponse(true);

      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));
      when(repository.save(any(UserChannelPreference.class))).thenReturn(preference);
      when(mapper.toResponse(preference)).thenReturn(response);

      UserPreferenceResponse result = service.create(TEST_USER_ID, request);

      assertThat(result).isEqualTo(response);
      verify(repository).save(any(UserChannelPreference.class));
    }

    @Test
    @DisplayName("should throw DuplicateResourceException on constraint violation")
    void shouldThrowOnDuplicate() {
      CreateUserPreferenceRequest request =
          new CreateUserPreferenceRequest(NotificationCategory.MARKETING, ChannelType.EMAIL, true);

      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));
      when(repository.save(any(UserChannelPreference.class)))
          .thenThrow(new DataIntegrityViolationException("duplicate"));

      assertThatThrownBy(() -> service.create(TEST_USER_ID, request))
          .isInstanceOf(DuplicateResourceException.class);
    }
  }

  // -------------------------------------------------------------------------
  // createDefaults
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("createDefaults")
  class CreateDefaults {

    @Test
    @DisplayName("should save all non-mandatory combinations when none exist")
    void shouldCreateAllDefaultsWhenNoneExist() {
      when(repository.findByUserIdAndOrganizationId(TEST_USER_ID, TEST_ORGANIZATION_ID))
          .thenReturn(List.of());

      service.createDefaults(TEST_USER_ID, TEST_ORGANIZATION_ID);

      // 2 non-mandatory categories × 4 channels = 8
      verify(repository).saveAll(anyList());
    }

    @Test
    @DisplayName("should skip existing keys and only save missing ones")
    void shouldSkipExistingKeys() {
      UserChannelPreference existing =
          UserPreferenceFixtures.enabled(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING);
      when(repository.findByUserIdAndOrganizationId(TEST_USER_ID, TEST_ORGANIZATION_ID))
          .thenReturn(List.of(existing));

      service.createDefaults(TEST_USER_ID, TEST_ORGANIZATION_ID);

      verify(repository).saveAll(anyList());
    }

    @Test
    @DisplayName("should not call saveAll when all defaults already exist")
    void shouldNotSaveWhenAllDefaultsExist() {
      List<UserChannelPreference> allDefaults =
          List.of(NotificationCategory.MARKETING, NotificationCategory.OPERATIONAL).stream()
              .flatMap(
                  category ->
                      List.of(ChannelType.values()).stream()
                          .map(
                              channel ->
                                  UserPreferenceFixtures.enabled(
                                      TEST_USER_ID, TEST_ORGANIZATION_ID,
                                      channel, category)))
              .toList();

      when(repository.findByUserIdAndOrganizationId(TEST_USER_ID, TEST_ORGANIZATION_ID))
          .thenReturn(allDefaults);

      service.createDefaults(TEST_USER_ID, TEST_ORGANIZATION_ID);

      verify(repository, never()).saveAll(anyList());
    }
  }

  // -------------------------------------------------------------------------
  // delete
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("should delete a non-mandatory preference")
    void shouldDeletePreference() {
      UserChannelPreference preference =
          UserPreferenceFixtures.enabled(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING);
      when(repository.findByUserIdAndOrganizationIdAndChannelAndCategory(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING))
          .thenReturn(Optional.of(preference));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingRunnable.class));

      service.delete(TEST_USER_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);

      verify(repository).delete(preference);
    }

    @Test
    @DisplayName("should throw InvalidStateException when deleting a mandatory category")
    void shouldThrowOnMandatoryCategory() {
      assertThatThrownBy(
              () -> service.delete(TEST_USER_ID, ChannelType.EMAIL, NotificationCategory.SECURITY))
          .isInstanceOf(InvalidStateException.class);

      verify(repository, never()).delete(any(UserChannelPreference.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when preference not found")
    void shouldThrowWhenNotFound() {
      when(repository.findByUserIdAndOrganizationIdAndChannelAndCategory(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.delete(TEST_USER_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("existsByUserId")
  class ExistsByUserId {

    @Test
    @DisplayName("should throw ResourceNotFoundException when user has no preferences")
    void shouldThrowWhenUserNotFound() {
      when(repository.existsByUserId(TEST_USER_ID)).thenReturn(false);

      assertThatThrownBy(() -> service.existsByUserId(TEST_USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findByCompositeKeyResponse")
  class FindByCompositeKeyResponse {

    @Test
    @DisplayName("should return mapped response when preference found")
    void shouldReturnMappedResponse() {
      UserChannelPreference preference =
          UserPreferenceFixtures.enabled(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING);
      UserPreferenceResponse response = stubResponse(true);

      when(repository.findByUserIdAndOrganizationIdAndChannelAndCategory(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING))
          .thenReturn(Optional.of(preference));
      when(mapper.toResponse(preference)).thenReturn(response);

      UserPreferenceResponse result =
          service.findByCompositeKeyResponse(
              TEST_USER_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);

      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(repository.findByUserIdAndOrganizationIdAndChannelAndCategory(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.findByCompositeKeyResponse(
                      TEST_USER_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // -------------------------------------------------------------------------
  // isEnabled
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("isEnabled")
  class IsEnabled {

    private NotificationRequest request(
        String userId, ChannelType channel, NotificationCategory category) {
      return new NotificationRequest(
          null,
          category,
          channel,
          "event.type",
          TEST_ORGANIZATION_ID,
          "recipient@test.com",
          "ref-" + UUID.randomUUID(),
          userId,
          null,
          null,
          null);
    }

    @Test
    @DisplayName("should return true for mandatory category without checking preferences")
    void shouldReturnTrueForMandatoryCategory() {
      NotificationRequest req =
          request(TEST_USER_ID, ChannelType.EMAIL, NotificationCategory.SECURITY);

      assertThat(service.isEnabled(req)).isTrue();

      verify(organizationPreferenceService, never())
          .isChannelEnabledForCategory(anyString(), any(), any());
    }

    @Test
    @DisplayName("should return false when org preference has channel disabled")
    void shouldReturnFalseWhenOrgDisabled() {
      NotificationRequest req =
          request(TEST_USER_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      when(organizationPreferenceService.isChannelEnabledForCategory(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .thenReturn(false);

      assertThat(service.isEnabled(req)).isFalse();
    }

    @Test
    @DisplayName("should return true when no userId and org preference is enabled")
    void shouldReturnTrueWhenNoUserId() {
      NotificationRequest req = request(null, ChannelType.EMAIL, NotificationCategory.MARKETING);
      when(organizationPreferenceService.isChannelEnabledForCategory(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .thenReturn(true);

      assertThat(service.isEnabled(req)).isTrue();

      verify(repository, never())
          .findByUserIdAndOrganizationIdAndChannelAndCategory(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should return false when user preference is disabled")
    void shouldReturnFalseWhenUserPrefDisabled() {
      NotificationRequest req =
          request(TEST_USER_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      when(organizationPreferenceService.isChannelEnabledForCategory(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .thenReturn(true);
      UserChannelPreference disabled =
          UserPreferenceFixtures.disabled(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING);
      when(repository.findByUserIdAndOrganizationIdAndChannelAndCategory(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING))
          .thenReturn(Optional.of(disabled));

      assertThat(service.isEnabled(req)).isFalse();
    }

    @Test
    @DisplayName("should return true when org and user preferences are both enabled")
    void shouldReturnTrueWhenAllEnabled() {
      NotificationRequest req =
          request(TEST_USER_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      when(organizationPreferenceService.isChannelEnabledForCategory(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .thenReturn(true);
      UserChannelPreference enabled =
          UserPreferenceFixtures.enabled(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING);
      when(repository.findByUserIdAndOrganizationIdAndChannelAndCategory(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING))
          .thenReturn(Optional.of(enabled));

      assertThat(service.isEnabled(req)).isTrue();
    }
  }

  // -------------------------------------------------------------------------
  // search
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("should delegate to repository and return mapped page")
    void shouldReturnMappedPage() {
      UserChannelPreference preference =
          UserPreferenceFixtures.enabled(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING);
      UserPreferenceResponse response = stubResponse(true);

      Page<UserChannelPreference> page = new PageImpl<>(List.of(preference));
      when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
      when(mapper.toResponse(preference)).thenReturn(response);

      Page<UserPreferenceResponse> result = service.search(new ChannelPreferenceFilter());

      assertThat(result.getContent()).containsExactly(response);
    }
  }

  // -------------------------------------------------------------------------
  // update
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("should enable a disabled preference and return updated response")
    void shouldEnablePreference() {
      UserChannelPreference preference =
          UserPreferenceFixtures.disabled(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING);
      UserPreferenceResponse response = stubResponse(true);

      when(repository.findByUserIdAndOrganizationIdAndChannelAndCategory(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING))
          .thenReturn(Optional.of(preference));
      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));
      when(repository.save(preference)).thenReturn(preference);
      when(mapper.toResponse(preference)).thenReturn(response);

      UserPreferenceResponse result =
          service.update(
              TEST_USER_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING,
              new UpdateUserPreferenceRequest(true));

      assertThat(preference.getEnabled()).isTrue();
      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should disable an enabled preference and return updated response")
    void shouldDisablePreference() {
      UserChannelPreference preference =
          UserPreferenceFixtures.enabled(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING);
      UserPreferenceResponse response = stubResponse(false);

      when(repository.findByUserIdAndOrganizationIdAndChannelAndCategory(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING))
          .thenReturn(Optional.of(preference));
      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));
      when(repository.save(preference)).thenReturn(preference);
      when(mapper.toResponse(preference)).thenReturn(response);

      UserPreferenceResponse result =
          service.update(
              TEST_USER_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING,
              new UpdateUserPreferenceRequest(false));

      assertThat(preference.getEnabled()).isFalse();
      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when preference not found")
    void shouldThrowWhenNotFound() {
      when(repository.findByUserIdAndOrganizationIdAndChannelAndCategory(
              TEST_USER_ID,
              TEST_ORGANIZATION_ID,
              ChannelType.EMAIL,
              NotificationCategory.MARKETING))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.update(
                      TEST_USER_ID,
                      ChannelType.EMAIL,
                      NotificationCategory.MARKETING,
                      new UpdateUserPreferenceRequest(true)))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
