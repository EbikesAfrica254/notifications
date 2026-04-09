package com.ebikes.notifications.services.templates;

import static com.ebikes.notifications.constants.EventConstants.DomainEvents;

import java.util.List;
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
import com.ebikes.notifications.support.audit.AuditTemplate;
import com.ebikes.notifications.support.database.FilterUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class TemplateService {

  private final AuditTemplate auditTemplate;
  private final TemplateMapper mapper;
  private final TemplateRepository repository;

  @Transactional
  public TemplateResponse activate(UUID id) {
    Template template = findTemplateById(id);
    template.activate();

    Template saved =
        auditTemplate.execute(
            template,
            null,
            DomainEvents.Templates.ACTIVATED,
            () -> {
              Template result = repository.save(template);
              log.info("Template activated - id={} name={}", id, template.getName());
              return result;
            });
    return mapper.toResponse(saved);
  }

  @Transactional
  public TemplateResponse create(CreateTemplateRequest request) {
    validateNameUniqueness(request.name());

    Template template =
        Template.builder()
            .bodyTemplate(request.bodyTemplate())
            .channel(request.channel())
            .templateContentType(request.templateContentType())
            .isActive(true)
            .name(request.name())
            .subject(request.subject())
            .variableDefinitions(
                request.variableDefinitions() != null
                    ? List.copyOf(request.variableDefinitions())
                    : List.of())
            .build();

    Template saved =
        auditTemplate.execute(
            template,
            null,
            DomainEvents.Templates.CREATED,
            () -> {
              Template result = repository.save(template);
              log.info(
                  "Template created - id={} name={} channel={}",
                  result.getId(),
                  result.getName(),
                  result.getChannel());
              return result;
            });
    return mapper.toResponse(saved);
  }

  @Transactional
  public TemplateResponse deactivate(UUID id) {
    Template template = findTemplateById(id);
    template.deactivate();

    Template saved =
        auditTemplate.execute(
            template,
            null,
            DomainEvents.Templates.DEACTIVATED,
            () -> {
              Template result = repository.save(template);
              log.info("Template deactivated - id={} name={}", id, template.getName());
              return result;
            });
    return mapper.toResponse(saved);
  }

  public Page<TemplateSummaryResponse> findAll(TemplateFilter filter) {
    log.info("Searching for templates with filter: {}", filter);
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

    Template saved =
        auditTemplate.execute(
            template,
            null,
            DomainEvents.Templates.UPDATED,
            () -> {
              Template result = repository.save(template);
              log.info(
                  "Template updated - id={} name={} version={}",
                  id,
                  template.getName(),
                  result.getVersion());
              return result;
            });
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
