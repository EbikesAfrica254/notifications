package com.ebikes.notifications.services.templates;

import static com.ebikes.notifications.constants.ApplicationConstants.SYSTEM_ID;
import static com.ebikes.notifications.constants.EventConstants.EventTypes;
import static com.ebikes.notifications.constants.EventConstants.RoutingKeys;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.database.repositories.TemplateRepository;
import com.ebikes.notifications.database.specifications.TemplateSpecifications;
import com.ebikes.notifications.dtos.requests.filters.TemplateFilter;
import com.ebikes.notifications.dtos.requests.templates.CreateTemplateRequest;
import com.ebikes.notifications.dtos.requests.templates.UpdateTemplateRequest;
import com.ebikes.notifications.dtos.responses.templates.TemplateResponse;
import com.ebikes.notifications.dtos.responses.templates.TemplateSummaryResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.DuplicateResourceException;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.TemplateMapper;
import com.ebikes.notifications.publishers.AuditEventPublisher;
import com.ebikes.notifications.support.audit.AuditMetadataBuilder;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.database.FilterUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class TemplateService {

  private static final String TEMPLATE = "TEMPLATE";

  private final AuditEventPublisher auditEventPublisher;
  private final TemplateMapper mapper;
  private final TemplateRepository repository;

  @Transactional
  public TemplateResponse activate(UUID id) {
    Template template = findTemplateById(id);
    template.activate();
    Template saved = repository.save(template);

    log.info("Template activated - id={} name={}", id, template.getName());

    auditEventPublisher.publishSuccess(
        template.getId(),
        TEMPLATE,
        EventTypes.Templates.ACTIVATED,
        AuditMetadataBuilder.forTemplate(saved),
        SYSTEM_ID,
        RoutingKeys.NOTIFICATIONS_TEMPLATE_AUDIT,
        ExecutionContext.getUserId());

    return mapper.toResponse(saved);
  }

  @Transactional
  public TemplateResponse create(CreateTemplateRequest request) {
    validateNameUniqueness(request.name());

    Template template =
        Template.builder()
            .bodyTemplate(request.bodyTemplate())
            .channel(request.channel())
            .contentType(request.contentType())
            .name(request.name())
            .subject(request.subject())
            .variableDefinitions(request.variableDefinitions())
            .build();

    Template saved = repository.save(template);

    log.info(
        "Template created - id={} name={} channel={}",
        saved.getId(),
        saved.getName(),
        saved.getChannel());

    auditEventPublisher.publishSuccess(
        template.getId(),
        TEMPLATE,
        EventTypes.Templates.CREATED,
        AuditMetadataBuilder.forTemplate(saved),
        SYSTEM_ID,
        RoutingKeys.NOTIFICATIONS_TEMPLATE_AUDIT,
        ExecutionContext.getUserId());

    return mapper.toResponse(saved);
  }

  @Transactional
  public TemplateResponse deactivate(UUID id) {
    Template template = findTemplateById(id);
    template.deactivate();
    Template saved = repository.save(template);

    log.info("Template deactivated - id={} name={}", id, template.getName());

    auditEventPublisher.publishSuccess(
        template.getId(),
        TEMPLATE,
        EventTypes.Templates.DEACTIVATED,
        AuditMetadataBuilder.forTemplate(saved),
        SYSTEM_ID,
        RoutingKeys.NOTIFICATIONS_TEMPLATE_AUDIT,
        ExecutionContext.getUserId());

    return mapper.toResponse(saved);
  }

  public Page<TemplateSummaryResponse> findAll(TemplateFilter filter) {
    Specification<Template> specification = TemplateSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, TemplateSpecifications.ALLOWED_SORT_FIELDS);

    return repository.findAll(specification, pageable).map(mapper::toSummaryResponse);
  }

  public Template findByChannelAndName(ChannelType channel, String name) {
    return repository
        .findByChannelAndNameAndIsActive(channel, name, true)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Template with channel=" + channel + " and name=" + name + " not found"));
  }

  public TemplateResponse findById(UUID id) {
    return mapper.toResponse(findTemplateById(id));
  }

  @Transactional
  public TemplateResponse update(UUID id, UpdateTemplateRequest request) {
    Template template = findTemplateById(id);
    template.updateContent(
        request.bodyTemplate(), request.subject(), request.variableDefinitions());
    Template saved = repository.save(template);

    log.info(
        "Template updated - id={} name={} version={}", id, template.getName(), saved.getVersion());

    auditEventPublisher.publishSuccess(
        template.getId(),
        TEMPLATE,
        EventTypes.Templates.UPDATED,
        AuditMetadataBuilder.forTemplate(saved),
        SYSTEM_ID,
        RoutingKeys.NOTIFICATIONS_TEMPLATE_AUDIT,
        ExecutionContext.getUserId());

    return mapper.toResponse(saved);
  }

  private Template findTemplateById(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Template with id=" + id + " not found"));
  }

  private void validateNameUniqueness(String name) {
    if (repository.existsByName(name)) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE, "Template with name=" + name + " already exists");
    }
  }
}
