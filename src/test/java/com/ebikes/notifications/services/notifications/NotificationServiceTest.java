package com.ebikes.notifications.services.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.database.repositories.NotificationRepository;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.dtos.requests.filters.NotificationFilter;
import com.ebikes.notifications.dtos.responses.notifications.NotificationResponse;
import com.ebikes.notifications.dtos.responses.notifications.NotificationSummaryResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.enums.NotificationStatus;
import com.ebikes.notifications.exceptions.DuplicateResourceException;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.NotificationMapper;
import com.ebikes.notifications.services.templates.TemplateProcessor;
import com.ebikes.notifications.services.templates.TemplateService;
import com.ebikes.notifications.services.templates.variables.VariableEnricher;
import com.ebikes.notifications.support.audit.AuditTemplate;
import com.ebikes.notifications.support.audit.ThrowingRunnable;
import com.ebikes.notifications.support.audit.ThrowingSupplier;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;
import com.ebikes.notifications.support.fixtures.SecurityFixtures;
import com.ebikes.notifications.support.fixtures.TemplateFixtures;

import tools.jackson.databind.ObjectMapper;

@DisplayName("NotificationService")
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final String TEST_ORGANIZATION_ID = SecurityFixtures.TEST_ORGANIZATION_ID;
  private static final String TEST_EMAIL = SecurityFixtures.TEST_EMAIL;
  private static final UUID NOTIFICATION_ID = UUID.randomUUID();

  @Mock private AuditTemplate auditTemplate;
  @Mock private NotificationMapper mapper;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotificationRepository repository;
  @Mock private TemplateProcessor templateProcessor;
  @Mock private TemplateService templateService;
  @Mock private VariableEnricher variableEnricher;

  private NotificationService service;

  @BeforeEach
  void setUp() {
    service =
        new NotificationService(
            auditTemplate,
            mapper,
            objectMapper,
            repository,
            templateProcessor,
            templateService,
            variableEnricher);
  }

  // -------------------------------------------------------------------------
  // cancel
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("cancel")
  class Cancel {

    @Test
    @DisplayName("should cancel a PENDING notification and save")
    @SuppressWarnings("unchecked")
    void shouldCancelPendingNotification() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingRunnable.class));

      service.cancel(NOTIFICATION_ID);

      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
      verify(repository).save(notification);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when notification not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.cancel(NOTIFICATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(repository, never()).save(any());
    }
  }

  // -------------------------------------------------------------------------
  // create — via template (EMAIL)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should create notification from template for non-SSE channel")
    @SuppressWarnings("unchecked")
    void shouldCreateFromTemplate() {
      Template template = TemplateFixtures.activeEmail("ORDER_COMPLETED");
      NotificationRequest request =
          new NotificationRequest(
              null,
              NotificationCategory.TRANSACTIONAL,
              ChannelType.EMAIL,
              "orders.completed",
              TEST_ORGANIZATION_ID,
              TEST_EMAIL,
              "ref-" + UUID.randomUUID(),
              null,
              "ORDER_COMPLETED",
              null,
              Map.of());

      when(repository.findByServiceReference(request.serviceReference()))
          .thenReturn(Optional.empty());
      when(templateService.findByChannelAndName(ChannelType.EMAIL, "ORDER_COMPLETED"))
          .thenReturn(template);
      when(variableEnricher.enrich(any(), any())).thenReturn(Map.of());
      when(templateProcessor.render(any(), anyString(), any(), any())).thenReturn("Rendered body");
      when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));

      Notification result = service.create(request);

      assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
      assertThat(result.getChannel()).isEqualTo(ChannelType.EMAIL);
      verify(repository).save(any(Notification.class));
    }

    @Test
    @DisplayName("should create notification from variables for SSE channel")
    @SuppressWarnings("unchecked")
    void shouldCreateFromVariablesForSse() {
      Map<String, Serializable> variables =
          Map.of(
              "userId", "user-1",
              "type", "ORDER_UPDATE",
              "entityId", "order-1",
              "reference", "ref-1",
              "title", "Order Updated",
              "message", "Your order was updated",
              "priority", "NORMAL",
              "metadata", (Serializable) Map.of());

      NotificationRequest request =
          new NotificationRequest(
              null,
              NotificationCategory.OPERATIONAL,
              ChannelType.SSE,
              "orders.updated",
              TEST_ORGANIZATION_ID,
              "user-1",
              "ref-" + UUID.randomUUID(),
              null,
              null,
              null,
              variables);

      com.ebikes.notifications.dtos.requests.channels.sse.SseRequest sseRequest =
          new com.ebikes.notifications.dtos.requests.channels.sse.SseRequest(
              "user-1",
              "ORDER_UPDATE",
              "order-1",
              "ref-1",
              "Order Updated",
              "Your order was updated",
              com.ebikes.notifications.dtos.requests.channels.sse.SseRequest.Priority.NORMAL,
              Map.of(),
              null);

      when(repository.findByServiceReference(request.serviceReference()))
          .thenReturn(Optional.empty());
      when(objectMapper.convertValue(any(), any(Class.class))).thenReturn(sseRequest);
      when(objectMapper.writeValueAsString(sseRequest)).thenReturn("{\"type\":\"ORDER_UPDATE\"}");
      when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
      doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingSupplier.class));

      Notification result = service.create(request);

      assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
      assertThat(result.getChannel()).isEqualTo(ChannelType.SSE);
      verify(templateService, never()).findByChannelAndName(any(), any());
      verify(repository).save(any(Notification.class));
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when serviceReference already exists")
    void shouldThrowOnDuplicateServiceReference() {
      Notification existing =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      NotificationRequest request =
          new NotificationRequest(
              null,
              NotificationCategory.TRANSACTIONAL,
              ChannelType.EMAIL,
              "orders.completed",
              TEST_ORGANIZATION_ID,
              TEST_EMAIL,
              "ref-duplicate",
              null,
              "ORDER_COMPLETED",
              null,
              Map.of());

      when(repository.findByServiceReference("ref-duplicate")).thenReturn(Optional.of(existing));

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(DuplicateResourceException.class);

      verify(repository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should return mapped response when notification found")
    void shouldReturnMappedResponse() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      NotificationResponse response =
          new NotificationResponse(
              null,
              ChannelType.EMAIL,
              null,
              null,
              NOTIFICATION_ID,
              "body",
              "subject",
              TEST_ORGANIZATION_ID,
              TEST_EMAIL,
              "ref-1",
              NotificationStatus.PENDING,
              null,
              null,
              null,
              null,
              null);

      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
      when(mapper.toResponse(notification)).thenReturn(response);

      NotificationResponse result = service.findById(NOTIFICATION_ID);

      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when notification not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findById(NOTIFICATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
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
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      NotificationSummaryResponse summary =
          new NotificationSummaryResponse(
              null,
              ChannelType.EMAIL,
              null,
              null,
              NOTIFICATION_ID,
              null,
              TEST_EMAIL,
              "ref-1",
              NotificationStatus.PENDING,
              null,
              null,
              null,
              null);

      Page<Notification> page = new PageImpl<>(List.of(notification));
      when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
      when(mapper.toSummaryResponse(notification)).thenReturn(summary);

      NotificationFilter filter = new NotificationFilter();
      Page<NotificationSummaryResponse> result = service.search(filter);

      assertThat(result.getContent()).containsExactly(summary);
    }
  }

  @Nested
  @DisplayName("markProcessing")
  class MarkProcessing {

    @Test
    @DisplayName("should mark PENDING notification as PROCESSING and save without audit")
    @SuppressWarnings("unchecked")
    void shouldMarkProcessingAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

      service.markProcessing(NOTIFICATION_ID);

      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
      verify(repository).save(notification);
      verify(auditTemplate, never()).execute(any(), any(), any(), any(ThrowingRunnable.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when notification not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.markProcessing(NOTIFICATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("markDelivered")
  class MarkDelivered {

    @Test
    @DisplayName("should mark PROCESSING notification as DELIVERED and save")
    @SuppressWarnings("unchecked")
    void shouldMarkDeliveredAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      notification.markProcessing();
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingRunnable.class));

      service.markDelivered(NOTIFICATION_ID);

      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
      verify(repository).save(notification);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when notification not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.markDelivered(NOTIFICATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("markFailed")
  class MarkFailed {

    @Test
    @DisplayName("should mark PROCESSING notification as FAILED and save")
    @SuppressWarnings("unchecked")
    void shouldMarkFailedAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      notification.markProcessing();
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
      doAnswer(
              inv -> {
                inv.getArgument(3, ThrowingRunnable.class).run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingRunnable.class));

      service.markFailed(NOTIFICATION_ID);

      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
      verify(repository).save(notification);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when notification not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.markFailed(NOTIFICATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("scheduleRetry")
  class ScheduleRetry {

    @Test
    @DisplayName("should mark FAILED notification back to PENDING and save without audit")
    @SuppressWarnings("unchecked")
    void shouldScheduleRetryAndSave() {
      Notification notification =
          NotificationFixtures.forOrganization(TEST_ORGANIZATION_ID, TEST_EMAIL);
      notification.markProcessing();
      notification.markFailed();
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

      service.scheduleRetry(NOTIFICATION_ID);

      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
      verify(repository).save(notification);
      verify(auditTemplate, never()).execute(any(), any(), any(), any(ThrowingRunnable.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when notification not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.scheduleRetry(NOTIFICATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
