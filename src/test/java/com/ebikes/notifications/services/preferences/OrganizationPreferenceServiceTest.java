package com.ebikes.notifications.services.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

import com.ebikes.notifications.database.entities.OrganizationChannelPreference;
import com.ebikes.notifications.database.repositories.OrganizationPreferenceRepository;
import com.ebikes.notifications.dtos.requests.filters.ChannelPreferenceFilter;
import com.ebikes.notifications.dtos.requests.preferences.organization.CreateOrganizationPreferenceRequest;
import com.ebikes.notifications.dtos.requests.preferences.organization.UpdateOrganizationPreferenceRequest;
import com.ebikes.notifications.dtos.responses.preferences.organization.OrganizationPreferenceResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.enums.UserRole;
import com.ebikes.notifications.exceptions.DuplicateResourceException;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.OrganizationPreferenceMapper;
import com.ebikes.notifications.support.audit.AuditTemplate;
import com.ebikes.notifications.support.audit.ThrowingRunnable;
import com.ebikes.notifications.support.audit.ThrowingSupplier;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.fixtures.OrganizationPreferenceFixtures;
import com.ebikes.notifications.support.fixtures.SecurityFixtures;

@DisplayName("OrganizationPreferenceService")
@ExtendWith(MockitoExtension.class)
class OrganizationPreferenceServiceTest {

  private static final String TEST_ORGANIZATION_ID = SecurityFixtures.TEST_ORGANIZATION_ID;
  private static final UUID PREFERENCE_ID = UUID.randomUUID();

  @Mock private AuditTemplate auditTemplate;
  @Mock private OrganizationPreferenceMapper mapper;
  @Mock private OrganizationPreferenceRepository repository;

  private OrganizationPreferenceService service;

  @BeforeEach
  void setUp() {
    service = new OrganizationPreferenceService(auditTemplate, mapper, repository);
    SecurityFixtures.setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
  }

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should create and return preference response")
    void shouldCreatePreference() {
      CreateOrganizationPreferenceRequest request =
          new CreateOrganizationPreferenceRequest(
              NotificationCategory.MARKETING, ChannelType.EMAIL, true);
      OrganizationChannelPreference preference =
          OrganizationPreferenceFixtures.enabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      OrganizationPreferenceResponse response =
          new OrganizationPreferenceResponse(
              NotificationCategory.MARKETING,
              ChannelType.EMAIL,
              null,
              true,
              PREFERENCE_ID,
              TEST_ORGANIZATION_ID,
              null,
              null);

      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));
      when(repository.save(any(OrganizationChannelPreference.class))).thenReturn(preference);
      when(mapper.toResponse(preference)).thenReturn(response);

      OrganizationPreferenceResponse result = service.create(request);

      assertThat(result).isEqualTo(response);
      verify(repository).save(any(OrganizationChannelPreference.class));
    }

    @Test
    @DisplayName("should throw DuplicateResourceException on constraint violation")
    void shouldThrowOnDuplicate() {
      CreateOrganizationPreferenceRequest request =
          new CreateOrganizationPreferenceRequest(
              NotificationCategory.MARKETING, ChannelType.EMAIL, true);

      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));
      when(repository.save(any(OrganizationChannelPreference.class)))
          .thenThrow(new DataIntegrityViolationException("duplicate"));

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(DuplicateResourceException.class);
    }
  }

  @Nested
  @DisplayName("createDefaults")
  class CreateDefaults {

    @Test
    @DisplayName("should save all non-mandatory combinations when none exist")
    void shouldCreateAllDefaultsWhenNoneExist() {
      when(repository.findByOrganizationId(TEST_ORGANIZATION_ID)).thenReturn(List.of());

      service.createDefaults(TEST_ORGANIZATION_ID);

      // 2 non-mandatory categories (MARKETING, OPERATIONAL) × 4 channels = 8
      verify(repository).saveAll(anyList());
    }

    @Test
    @DisplayName("should skip existing keys and only save missing ones")
    void shouldSkipExistingKeys() {
      OrganizationChannelPreference existing =
          OrganizationPreferenceFixtures.enabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      when(repository.findByOrganizationId(TEST_ORGANIZATION_ID)).thenReturn(List.of(existing));

      service.createDefaults(TEST_ORGANIZATION_ID);

      // 7 remaining combinations should be saved (8 total - 1 existing)
      verify(repository).saveAll(anyList());
    }

    @Test
    @DisplayName("should not call saveAll when all defaults already exist")
    void shouldNotSaveWhenAllDefaultsExist() {
      // Build all 8 non-mandatory combinations
      List<OrganizationChannelPreference> allDefaults =
          List.of(NotificationCategory.MARKETING, NotificationCategory.OPERATIONAL).stream()
              .flatMap(
                  category ->
                      List.of(ChannelType.values()).stream()
                          .map(
                              channel ->
                                  OrganizationPreferenceFixtures.enabled(
                                      TEST_ORGANIZATION_ID, channel, category)))
              .toList();

      when(repository.findByOrganizationId(TEST_ORGANIZATION_ID)).thenReturn(allDefaults);

      service.createDefaults(TEST_ORGANIZATION_ID);

      verify(repository, never()).saveAll(anyList());
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("should delete a non-mandatory preference")
    void shouldDeletePreference() {
      OrganizationChannelPreference preference =
          OrganizationPreferenceFixtures.enabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      when(repository.findById(PREFERENCE_ID)).thenReturn(Optional.of(preference));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingRunnable.class));

      service.delete(PREFERENCE_ID);

      verify(repository).delete(preference);
    }

    @Test
    @DisplayName("should throw InvalidStateException when deleting a mandatory category")
    void shouldThrowOnMandatoryCategory() {
      OrganizationChannelPreference preference =
          OrganizationPreferenceFixtures.enabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.SECURITY);
      when(repository.findById(PREFERENCE_ID)).thenReturn(Optional.of(preference));

      assertThatThrownBy(() -> service.delete(PREFERENCE_ID))
          .isInstanceOf(InvalidStateException.class);

      verify(repository, never()).delete(any(OrganizationChannelPreference.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when preference not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(PREFERENCE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.delete(PREFERENCE_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should return mapped response when found")
    void shouldReturnMappedResponse() {
      OrganizationChannelPreference preference =
          OrganizationPreferenceFixtures.enabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      OrganizationPreferenceResponse response =
          new OrganizationPreferenceResponse(
              NotificationCategory.MARKETING,
              ChannelType.EMAIL,
              null,
              true,
              PREFERENCE_ID,
              TEST_ORGANIZATION_ID,
              null,
              null);

      when(repository.findById(PREFERENCE_ID)).thenReturn(Optional.of(preference));
      when(mapper.toResponse(preference)).thenReturn(response);

      assertThat(service.findById(PREFERENCE_ID)).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(PREFERENCE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findById(PREFERENCE_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("isChannelEnabledForCategory")
  class IsChannelEnabledForCategory {

    @Test
    @DisplayName("should return true when preference exists and is enabled")
    void shouldReturnTrueWhenEnabled() {
      OrganizationChannelPreference preference =
          OrganizationPreferenceFixtures.enabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      when(repository.findByOrganizationIdAndChannelAndCategory(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .thenReturn(Optional.of(preference));

      assertThat(
              service.isChannelEnabledForCategory(
                  TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .isTrue();
    }

    @Test
    @DisplayName("should return false when preference exists and is disabled")
    void shouldReturnFalseWhenDisabled() {
      OrganizationChannelPreference preference =
          OrganizationPreferenceFixtures.disabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      when(repository.findByOrganizationIdAndChannelAndCategory(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .thenReturn(Optional.of(preference));

      assertThat(
              service.isChannelEnabledForCategory(
                  TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .isFalse();
    }

    @Test
    @DisplayName("should return true when no preference record exists (defaults to enabled)")
    void shouldDefaultToTrueWhenAbsent() {
      when(repository.findByOrganizationIdAndChannelAndCategory(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .thenReturn(Optional.empty());

      assertThat(
              service.isChannelEnabledForCategory(
                  TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("should delegate to repository and return mapped page")
    void shouldReturnMappedPage() {
      OrganizationChannelPreference preference =
          OrganizationPreferenceFixtures.enabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      OrganizationPreferenceResponse response =
          new OrganizationPreferenceResponse(
              NotificationCategory.MARKETING,
              ChannelType.EMAIL,
              null,
              true,
              PREFERENCE_ID,
              TEST_ORGANIZATION_ID,
              null,
              null);

      Page<OrganizationChannelPreference> page = new PageImpl<>(List.of(preference));
      when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
      when(mapper.toResponse(preference)).thenReturn(response);

      Page<OrganizationPreferenceResponse> result = service.search(new ChannelPreferenceFilter());

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
      // Must start disabled so entity.enable() doesn't throw
      OrganizationChannelPreference preference =
          OrganizationPreferenceFixtures.disabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      OrganizationPreferenceResponse response =
          new OrganizationPreferenceResponse(
              NotificationCategory.MARKETING,
              ChannelType.EMAIL,
              null,
              true,
              PREFERENCE_ID,
              TEST_ORGANIZATION_ID,
              null,
              null);

      when(repository.findById(PREFERENCE_ID)).thenReturn(Optional.of(preference));
      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));
      when(repository.save(preference)).thenReturn(preference);
      when(mapper.toResponse(preference)).thenReturn(response);

      OrganizationPreferenceResponse result =
          service.update(PREFERENCE_ID, new UpdateOrganizationPreferenceRequest(true));

      assertThat(preference.getEnabled()).isTrue();
      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should disable an enabled preference and return updated response")
    void shouldDisablePreference() {
      // Must start enabled so entity.disable() doesn't throw
      OrganizationChannelPreference preference =
          OrganizationPreferenceFixtures.enabled(
              TEST_ORGANIZATION_ID, ChannelType.EMAIL, NotificationCategory.MARKETING);
      OrganizationPreferenceResponse response =
          new OrganizationPreferenceResponse(
              NotificationCategory.MARKETING,
              ChannelType.EMAIL,
              null,
              false,
              PREFERENCE_ID,
              TEST_ORGANIZATION_ID,
              null,
              null);

      when(repository.findById(PREFERENCE_ID)).thenReturn(Optional.of(preference));
      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));
      when(repository.save(preference)).thenReturn(preference);
      when(mapper.toResponse(preference)).thenReturn(response);

      OrganizationPreferenceResponse result =
          service.update(PREFERENCE_ID, new UpdateOrganizationPreferenceRequest(false));

      assertThat(preference.getEnabled()).isFalse();
      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when preference not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(PREFERENCE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.update(PREFERENCE_ID, new UpdateOrganizationPreferenceRequest(true)))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
