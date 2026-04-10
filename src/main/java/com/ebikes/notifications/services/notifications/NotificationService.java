package com.ebikes.notifications.services.notifications;

import static com.ebikes.notifications.constants.EventConstants.DomainEvents;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.database.repositories.NotificationRepository;
import com.ebikes.notifications.database.specifications.NotificationSpecifications;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.dtos.requests.channels.sse.SseRequest;
import com.ebikes.notifications.dtos.requests.filters.NotificationFilter;
import com.ebikes.notifications.dtos.responses.notifications.NotificationResponse;
import com.ebikes.notifications.dtos.responses.notifications.NotificationSummaryResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationStatus;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.enums.TemplateContentType;
import com.ebikes.notifications.exceptions.DuplicateResourceException;
import com.ebikes.notifications.exceptions.InvalidStateException;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.exceptions.ValidationException;
import com.ebikes.notifications.mappers.NotificationMapper;
import com.ebikes.notifications.services.templates.TemplateProcessor;
import com.ebikes.notifications.services.templates.TemplateService;
import com.ebikes.notifications.services.templates.TemplateVariableEnricher;
import com.ebikes.notifications.support.audit.AuditTemplate;
import com.ebikes.notifications.support.database.FilterUtilities;
import com.ebikes.notifications.support.security.RecipientMaskingUtility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

  private static final String VARIABLE_ENTITY_ID = "entityId";
  private static final String VARIABLE_REFERENCE = "reference";

  private final AuditTemplate auditTemplate;
  private final NotificationMapper mapper;
  private final ObjectMapper objectMapper;
  private final NotificationRepository repository;
  private final TemplateProcessor templateProcessor;
  private final TemplateService templateService;
  private final TemplateVariableEnricher templateVariableEnricher;

  @Transactional
  public void cancel(UUID id) {
    Notification notification = findNotificationById(id);
    notification.cancel();

    auditTemplate.execute(
        notification,
        notification.getOrganizationId(),
        DomainEvents.Notifications.CANCELLED,
        () -> {
          repository.save(notification);
          log.info(
              "Notification cancelled - id={} serviceReference={}",
              id,
              notification.getServiceReference());
        });
  }

  @Transactional
  public Notification create(NotificationRequest request) {
    deduplicate(request.serviceReference());

    Notification notification =
        request.channel() == ChannelType.SSE
            ? buildFromVariables(request)
            : buildFromTemplate(request);

    return auditTemplate.execute(
        notification,
        notification.getOrganizationId(),
        DomainEvents.Notifications.CREATED,
        () -> {
          Notification saved = repository.save(notification);
          log.info(
              "Notification created - id={} serviceReference={} organizationId={} branchId={}"
                  + " channel={} templateName={} templateVersion={}",
              saved.getId(),
              saved.getServiceReference(),
              saved.getOrganizationId(),
              saved.getBranchId(),
              saved.getChannel(),
              request.templateName(),
              saved.getTemplateVersion());
          return saved;
        });
  }

  @Transactional(readOnly = true)
  public Page<NotificationSummaryResponse> search(NotificationFilter filter) {
    Specification<Notification> specification =
        NotificationSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, NotificationSpecifications.ALLOWED_SORT_FIELDS);

    return repository.findAll(specification, pageable).map(mapper::toSummaryResponse);
  }

  @Transactional(readOnly = true)
  public NotificationResponse findById(UUID id) {
    return mapper.toResponse(findNotificationById(id));
  }

  @Transactional
  public void markProcessing(UUID id) {
    Notification notification = findNotificationById(id);
    notification.markProcessing();
    repository.save(notification);

    log.info(
        "Notification marked PROCESSING - id={} serviceReference={}",
        id,
        notification.getServiceReference());
  }

  @Transactional
  public void markDelivered(UUID id) {
    Notification notification = findNotificationById(id);
    notification.markDelivered();

    auditTemplate.execute(
        notification,
        notification.getOrganizationId(),
        DomainEvents.Notifications.DELIVERED,
        () -> {
          repository.save(notification);
          log.info(
              "Notification marked DELIVERED - id={} serviceReference={}",
              id,
              notification.getServiceReference());
        });
  }

  @Transactional
  public void markFailed(UUID id) {
    Notification notification = findNotificationById(id);
    notification.markFailed();

    auditTemplate.execute(
        notification,
        notification.getOrganizationId(),
        DomainEvents.Notifications.FAILED,
        () -> {
          repository.save(notification);
          log.info(
              "Notification marked FAILED - id={} serviceReference={}",
              id,
              notification.getServiceReference());
        });
  }

  @Transactional
  public void scheduleRetry(UUID id) {
    Notification notification = findNotificationById(id);
    notification.scheduleRetry();
    repository.save(notification);

    log.info(
        "Notification scheduled for retry - id={} serviceReference={}",
        id,
        notification.getServiceReference());
  }

  private Notification buildFromTemplate(NotificationRequest request) {
    Template template =
        templateService.findByChannelAndName(request.channel(), request.templateName());

    Map<String, Serializable> variables = templateVariableEnricher.enrich(request);

    String messageBody =
        templateProcessor.render(
            template.getTemplateContentType(),
            template.getBodyTemplate(),
            template.getVariableDefinitions(),
            variables);

    String messageSubject =
        template.getSubject() == null
            ? null
            : templateProcessor.render(
                TemplateContentType.PLAIN_TEXT,
                template.getSubject(),
                template.getVariableDefinitions(),
                variables);

    return Notification.builder()
        .branchId(request.branchId())
        .channel(request.channel())
        .messageBody(messageBody)
        .messageSubject(messageSubject)
        .organizationId(request.organizationId())
        .recipient(RecipientMaskingUtility.mask(request.recipient()))
        .serviceReference(request.serviceReference())
        .status(NotificationStatus.PENDING)
        .template(template)
        .templateVersion(template.getVersion())
        .variables(templateProcessor.redactVariables(variables, template.getVariableDefinitions()))
        .build();
  }

  private Notification buildFromVariables(NotificationRequest request) {
    SseRequest sseRequest;
    try {
      sseRequest = objectMapper.convertValue(request.variables(), SseRequest.class);
    } catch (Exception e) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "Invalid SSE payload for serviceReference=" + request.serviceReference(),
          "variables",
          null);
    }

    String messageBody;
    try {
      messageBody = objectMapper.writeValueAsString(sseRequest);
    } catch (Exception e) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE,
          "Failed to serialize SSE payload for serviceReference=" + request.serviceReference(),
          e);
    }

    return Notification.builder()
        .branchId(request.branchId())
        .channel(request.channel())
        .messageBody(messageBody)
        .messageSubject(null)
        .organizationId(request.organizationId())
        .recipient(RecipientMaskingUtility.mask(request.recipient()))
        .serviceReference(request.serviceReference())
        .status(NotificationStatus.PENDING)
        .variables(request.variables())
        .build();
  }

  private void deduplicate(String serviceReference) {
    repository
        .findByServiceReference(serviceReference)
        .ifPresent(
            existing -> {
              log.warn(
                  "Duplicate notification request rejected - serviceReference={} existingId={}",
                  serviceReference,
                  existing.getId());
              throw new DuplicateResourceException(
                  ResponseCode.DUPLICATE_RESOURCE,
                  "Notification with serviceReference " + serviceReference + " already exists");
            });
  }

  private Notification findNotificationById(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Notification with id=" + id + " not found"));
  }
}
