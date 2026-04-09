package com.ebikes.notifications.services.preferences;

import static com.ebikes.notifications.constants.EventConstants.DomainEvents;
import static com.ebikes.notifications.constants.PreferenceConstants.MANDATORY_CATEGORIES;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.notifications.database.entities.OrganizationChannelPreference;
import com.ebikes.notifications.database.repositories.OrganizationPreferenceRepository;
import com.ebikes.notifications.database.specifications.OrganizationPreferenceSpecifications;
import com.ebikes.notifications.dtos.internal.PreferenceKey;
import com.ebikes.notifications.dtos.requests.filters.ChannelPreferenceFilter;
import com.ebikes.notifications.dtos.requests.preferences.organization.CreateOrganizationPreferenceRequest;
import com.ebikes.notifications.dtos.requests.preferences.organization.UpdateOrganizationPreferenceRequest;
import com.ebikes.notifications.dtos.responses.preferences.organization.OrganizationPreferenceResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.DuplicateResourceException;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.OrganizationPreferenceMapper;
import com.ebikes.notifications.support.audit.AuditTemplate;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.database.FilterUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrganizationPreferenceService {

  private final AuditTemplate auditTemplate;
  private final OrganizationPreferenceMapper mapper;
  private final OrganizationPreferenceRepository repository;

  @Transactional
  public OrganizationPreferenceResponse create(CreateOrganizationPreferenceRequest request) {
    String organizationId =
        switch (ExecutionContext.get()) {
          case ExecutionContext.UserContext uc -> uc.activeOrganization();
          case ExecutionContext.SystemContext ignored ->
              throw new IllegalStateException("activeOrganization() called in system context");
        };

    OrganizationChannelPreference preference =
        OrganizationChannelPreference.builder()
            .category(request.category())
            .channel(request.channel())
            .enabled(request.enabled())
            .organizationId(organizationId)
            .build();

    try {
      OrganizationChannelPreference saved =
          auditTemplate.execute(
              preference,
              organizationId,
              DomainEvents.Preferences.Organization.CREATED,
              () -> {
                OrganizationChannelPreference result = repository.save(preference);
                log.info(
                    "Organization preference created - organizationId={} channel={} category={}"
                        + " enabled={}",
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
          "Organization preference already exists for organizationId="
              + organizationId
              + " channel="
              + request.channel()
              + " category="
              + request.category());
    }
  }

  @Transactional
  public void createDefaults(String organizationId) {
    Set<PreferenceKey> existingKeys =
        repository.findByOrganizationId(organizationId).stream()
            .map(pref -> new PreferenceKey(pref.getChannel(), pref.getCategory()))
            .collect(Collectors.toSet());

    List<OrganizationChannelPreference> newPreferences = new ArrayList<>();

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
            OrganizationChannelPreference.builder()
                .category(category)
                .channel(channel)
                .enabled(true)
                .organizationId(organizationId)
                .build());
      }
    }

    if (!newPreferences.isEmpty()) {
      repository.saveAll(newPreferences);
    }

    log.info(
        "Default preferences created - organizationId={} count={}",
        organizationId,
        newPreferences.size());
  }

  @Transactional
  public void delete(UUID id) {
    OrganizationChannelPreference preference = requiredById(id);

    if (MANDATORY_CATEGORIES.contains(preference.getCategory())) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE,
          "Cannot delete mandatory category preference: " + preference.getCategory());
    }

    auditTemplate.execute(
        preference,
        preference.getOrganizationId(),
        DomainEvents.Preferences.Organization.DELETED,
        () -> {
          repository.delete(preference);
          log.info(
              "Organization preference deleted - id={} organizationId={} channel={} category={}",
              id,
              preference.getOrganizationId(),
              preference.getChannel(),
              preference.getCategory());
        });
  }

  @Transactional(readOnly = true)
  public Page<OrganizationPreferenceResponse> search(ChannelPreferenceFilter filter) {
    Specification<OrganizationChannelPreference> specification =
        OrganizationPreferenceSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(
            filter, OrganizationPreferenceSpecifications.ALLOWED_SORT_FIELDS);

    return repository.findAll(specification, pageable).map(mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public OrganizationPreferenceResponse findById(UUID id) {
    return mapper.toResponse(requiredById(id));
  }

  @Transactional(readOnly = true)
  boolean isChannelEnabledForCategory(
      String organizationId, ChannelType channel, NotificationCategory category) {
    return repository
        .findByOrganizationIdAndChannelAndCategory(organizationId, channel, category)
        .map(OrganizationChannelPreference::getEnabled)
        .orElse(Boolean.TRUE);
  }

  @Transactional
  public OrganizationPreferenceResponse update(
      UUID id, UpdateOrganizationPreferenceRequest request) {
    OrganizationChannelPreference preference = requiredById(id);

    if (Boolean.TRUE.equals(request.enabled())) {
      preference.enable();
    } else {
      preference.disable();
    }

    OrganizationChannelPreference saved =
        auditTemplate.execute(
            preference,
            preference.getOrganizationId(),
            DomainEvents.Preferences.Organization.UPDATED,
            () -> {
              OrganizationChannelPreference result = repository.save(preference);
              log.info(
                  "Organization preference updated - id={} organizationId={} channel={} category={}"
                      + " enabled={}",
                  id,
                  preference.getOrganizationId(),
                  preference.getChannel(),
                  preference.getCategory(),
                  request.enabled());
              return result;
            });
    return mapper.toResponse(saved);
  }

  private OrganizationChannelPreference requiredById(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Organization preference with id " + id + " not found"));
  }
}
