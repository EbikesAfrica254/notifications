package com.ebikes.notifications.services.preferences;

import static com.ebikes.notifications.constants.EventConstants.DomainEvents;
import static com.ebikes.notifications.constants.PreferenceConstants.MANDATORY_CATEGORIES;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotBlank;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.notifications.database.entities.UserChannelPreference;
import com.ebikes.notifications.database.repositories.UserPreferenceRepository;
import com.ebikes.notifications.database.specifications.UserPreferenceSpecifications;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.dtos.internal.PreferenceKey;
import com.ebikes.notifications.dtos.requests.filters.ChannelPreferenceFilter;
import com.ebikes.notifications.dtos.requests.preferences.user.CreateUserPreferenceRequest;
import com.ebikes.notifications.dtos.requests.preferences.user.UpdateUserPreferenceRequest;
import com.ebikes.notifications.dtos.responses.preferences.user.UserPreferenceResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.DuplicateResourceException;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.UserPreferenceMapper;
import com.ebikes.notifications.support.audit.AuditTemplate;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.database.FilterUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserPreferenceService {

  private final AuditTemplate auditTemplate;
  private final OrganizationPreferenceService organizationPreferenceService;
  private final UserPreferenceMapper mapper;
  private final UserPreferenceRepository repository;

  @Transactional
  public UserPreferenceResponse create(String userId, CreateUserPreferenceRequest request) {
    String organizationId =
        switch (ExecutionContext.get()) {
          case ExecutionContext.UserContext uc -> uc.activeOrganization();
          case ExecutionContext.SystemContext ignored ->
              throw new IllegalStateException("activeOrganization() called in system context");
        };

    UserChannelPreference preference =
        UserChannelPreference.builder()
            .category(request.category())
            .channel(request.channel())
            .enabled(request.enabled())
            .organizationId(organizationId)
            .userId(userId)
            .build();

    try {
      UserChannelPreference saved =
          auditTemplate.execute(
              preference,
              organizationId,
              DomainEvents.Preferences.User.CREATED,
              () -> {
                UserChannelPreference result = repository.save(preference);
                log.info(
                    "User preference created - userId={} organizationId={} channel={} category={}"
                        + " enabled={}",
                    userId,
                    organizationId,
                    request.channel(),
                    request.category(),
                    result.getEnabled());
                return result;
              });
      return mapper.toResponse(saved);
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "User preference already exists for userId="
              + userId
              + " organizationId="
              + organizationId
              + " channel="
              + request.channel()
              + " category="
              + request.category());
    }
  }

  @Transactional
  public void createDefaults(String userId, String organizationId) {
    Set<PreferenceKey> existingKeys =
        repository.findByUserIdAndOrganizationId(userId, organizationId).stream()
            .map(pref -> new PreferenceKey(pref.getChannel(), pref.getCategory()))
            .collect(Collectors.toSet());

    List<UserChannelPreference> newPreferences = new ArrayList<>();

    for (NotificationCategory category : NotificationCategory.values()) {
      if (MANDATORY_CATEGORIES.contains(category)) {
        continue;
      }
      for (ChannelType channel : ChannelType.values()) {
        PreferenceKey key = new PreferenceKey(channel, category);
        if (existingKeys.contains(key)) {
          continue;
        }
        newPreferences.add(
            UserChannelPreference.builder()
                .category(category)
                .channel(channel)
                .enabled(true)
                .organizationId(organizationId)
                .userId(userId)
                .build());
      }
    }

    if (!newPreferences.isEmpty()) {
      repository.saveAll(newPreferences);
    }

    log.info(
        "Default preferences created - userId={} organizationId={} count={}",
        userId,
        organizationId,
        newPreferences.size());
  }

  @Transactional
  public void delete(String userId, ChannelType channel, NotificationCategory category) {
    String organizationId =
        switch (ExecutionContext.get()) {
          case ExecutionContext.UserContext uc -> uc.activeOrganization();
          case ExecutionContext.SystemContext ignored ->
              throw new IllegalStateException("activeOrganization() called in system context");
        };

    if (MANDATORY_CATEGORIES.contains(category)) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE, "Cannot delete mandatory category preference: " + category);
    }

    UserChannelPreference preference =
        findByCompositeKey(userId, organizationId, channel, category);

    auditTemplate.execute(
        preference,
        preference.getOrganizationId(),
        DomainEvents.Preferences.User.DELETED,
        () -> {
          repository.delete(preference);
          log.info(
              "User preference deleted - userId={} organizationId={} channel={} category={}",
              userId,
              organizationId,
              channel,
              category);
        });
  }

  @Transactional(readOnly = true)
  public void existsByUserId(@NotBlank String userId) {
    if (!repository.existsByUserId(userId)) {
      throw new ResourceNotFoundException(
          ResponseCode.RESOURCE_NOT_FOUND, "User preference not found for userId=" + userId);
    }
  }

  @Transactional(readOnly = true)
  public Page<UserPreferenceResponse> search(ChannelPreferenceFilter filter) {
    Specification<UserChannelPreference> specification =
        UserPreferenceSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, UserPreferenceSpecifications.ALLOWED_SORT_FIELDS);

    return repository.findAll(specification, pageable).map(mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public UserPreferenceResponse findByCompositeKeyResponse(
      String userId, ChannelType channel, NotificationCategory category) {
    String organizationId =
        switch (ExecutionContext.get()) {
          case ExecutionContext.UserContext uc -> uc.activeOrganization();
          case ExecutionContext.SystemContext ignored ->
              throw new IllegalStateException("activeOrganization() called in system context");
        };
    return mapper.toResponse(findByCompositeKey(userId, organizationId, channel, category));
  }

  @Transactional(readOnly = true)
  public boolean isEnabled(NotificationRequest request) {
    NotificationCategory category = request.category();
    ChannelType channel = request.channel();
    String organizationId = request.organizationId();
    String userId = request.subjectUserId();
    if (MANDATORY_CATEGORIES.contains(category)) {
      log.debug(
          "Mandatory category - bypassing preference check channel={} category={}",
          channel,
          category);
      return true;
    }

    if (!organizationPreferenceService.isChannelEnabledForCategory(
        organizationId, channel, category)) {
      log.info(
          "Channel suppressed by organization preference - organizationId={} channel={}"
              + " category={}",
          organizationId,
          channel,
          category);
      return false;
    }

    if (userId == null || userId.isBlank()) {
      log.debug(
          "No userId on request - skipping user preference check channel={} category={}",
          channel,
          category);
      return true;
    }

    return repository
        .findByUserIdAndOrganizationIdAndChannelAndCategory(
            userId, organizationId, channel, category)
        .map(UserChannelPreference::getEnabled)
        .orElse(Boolean.TRUE);
  }

  @Transactional
  public UserPreferenceResponse update(
      String userId,
      ChannelType channel,
      NotificationCategory category,
      UpdateUserPreferenceRequest request) {
    String organizationId =
        switch (ExecutionContext.get()) {
          case ExecutionContext.UserContext uc -> uc.activeOrganization();
          case ExecutionContext.SystemContext ignored ->
              throw new IllegalStateException("activeOrganization() called in system context");
        };

    UserChannelPreference preference =
        findByCompositeKey(userId, organizationId, channel, category);

    if (Boolean.TRUE.equals(request.enabled())) {
      preference.enable();
    } else {
      preference.disable();
    }

    UserChannelPreference saved =
        auditTemplate.execute(
            preference,
            preference.getOrganizationId(),
            DomainEvents.Preferences.User.UPDATED,
            () -> {
              UserChannelPreference result = repository.save(preference);
              log.info(
                  "User preference updated - userId={} organizationId={} channel={} category={}"
                      + " enabled={}",
                  userId,
                  organizationId,
                  channel,
                  category,
                  request.enabled());
              return result;
            });
    return mapper.toResponse(saved);
  }

  private UserChannelPreference findByCompositeKey(
      String userId, String organizationId, ChannelType channel, NotificationCategory category) {
    return repository
        .findByUserIdAndOrganizationIdAndChannelAndCategory(
            userId, organizationId, channel, category)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "User preference not found for userId="
                        + userId
                        + " organizationId="
                        + organizationId
                        + " channel="
                        + channel
                        + " category="
                        + category));
  }
}
