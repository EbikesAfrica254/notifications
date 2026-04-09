package com.ebikes.notifications.services.templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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

import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.database.repositories.TemplateRepository;
import com.ebikes.notifications.dtos.requests.filters.TemplateFilter;
import com.ebikes.notifications.dtos.requests.templates.CreateTemplateRequest;
import com.ebikes.notifications.dtos.requests.templates.UpdateTemplateRequest;
import com.ebikes.notifications.dtos.responses.templates.TemplateResponse;
import com.ebikes.notifications.dtos.responses.templates.TemplateSummaryResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.TemplateContentType;
import com.ebikes.notifications.exceptions.DuplicateResourceException;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.mappers.TemplateMapper;
import com.ebikes.notifications.support.audit.AuditTemplate;
import com.ebikes.notifications.support.audit.ThrowingSupplier;
import com.ebikes.notifications.support.fixtures.TemplateFixtures;

@DisplayName("TemplateService")
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

  private static final UUID TEMPLATE_ID = UUID.randomUUID();

  @Mock private AuditTemplate auditTemplate;
  @Mock private TemplateMapper mapper;
  @Mock private TemplateRepository repository;

  private TemplateService service;

  @BeforeEach
  void setUp() {
    service = new TemplateService(auditTemplate, mapper, repository);
  }

  private void stubAuditSupplier() {
    doAnswer(inv -> inv.getArgument(3, ThrowingSupplier.class).get())
        .when(auditTemplate)
        .execute(any(), any(), any(), any(ThrowingSupplier.class));
  }

  private TemplateResponse stubResponse(Template template) {
    TemplateResponse response =
        new TemplateResponse(
            template.getBodyTemplate(),
            template.getChannel(),
            template.getTemplateContentType(),
            null,
            null,
            TEMPLATE_ID,
            template.isActive(),
            template.getName(),
            template.getSubject(),
            null,
            null,
            template.getVariableDefinitions(),
            0);
    when(mapper.toResponse(template)).thenReturn(response);
    return response;
  }

  @Nested
  @DisplayName("activate")
  class Activate {

    @Test
    @DisplayName("should activate an inactive template and return response")
    void shouldActivateTemplate() {
      Template template = TemplateFixtures.inactive("EMAIL_WELCOME");
      when(repository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
      stubAuditSupplier();
      when(repository.save(template)).thenReturn(template);
      TemplateResponse response = stubResponse(template);

      TemplateResponse result = service.activate(TEMPLATE_ID);

      assertThat(template.isActive()).isTrue();
      assertThat(result).isEqualTo(response);
      verify(repository).save(template);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when template not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.activate(TEMPLATE_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should create and return template response")
    void shouldCreateTemplate() {
      CreateTemplateRequest request =
          new CreateTemplateRequest(
              "Hello [[${name}]]",
              ChannelType.EMAIL,
              TemplateContentType.HTML,
              "EMAIL_WELCOME",
              "Welcome",
              List.of());
      Template template = TemplateFixtures.activeEmail("EMAIL_WELCOME");

      when(repository.existsByName("EMAIL_WELCOME")).thenReturn(false);
      stubAuditSupplier();
      when(repository.save(any(Template.class))).thenReturn(template);
      TemplateResponse response = stubResponse(template);

      TemplateResponse result = service.create(request);

      assertThat(result).isEqualTo(response);
      verify(repository).save(any(Template.class));
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when name already exists")
    void shouldThrowOnDuplicateName() {
      CreateTemplateRequest request =
          new CreateTemplateRequest(
              "Hello",
              ChannelType.EMAIL,
              TemplateContentType.HTML,
              "EMAIL_WELCOME",
              "Welcome",
              List.of());
      when(repository.existsByName("EMAIL_WELCOME")).thenReturn(true);

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(DuplicateResourceException.class);
    }
  }

  @Nested
  @DisplayName("deactivate")
  class Deactivate {

    @Test
    @DisplayName("should deactivate an active template and return response")
    void shouldDeactivateTemplate() {
      Template template = TemplateFixtures.activeEmail("EMAIL_WELCOME");
      when(repository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
      stubAuditSupplier();
      when(repository.save(template)).thenReturn(template);
      TemplateResponse response = stubResponse(template);

      TemplateResponse result = service.deactivate(TEMPLATE_ID);

      assertThat(template.isActive()).isFalse();
      assertThat(result).isEqualTo(response);
      verify(repository).save(template);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when template not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deactivate(TEMPLATE_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findAll")
  class FindAll {

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("should delegate to repository and return mapped page")
    void shouldReturnMappedPage() {
      Template template = TemplateFixtures.activeEmail("EMAIL_WELCOME");
      TemplateSummaryResponse summary =
          new TemplateSummaryResponse(
              ChannelType.EMAIL,
              TemplateContentType.HTML,
              null,
              null,
              TEMPLATE_ID,
              true,
              "EMAIL_WELCOME",
              null,
              null,
              0);

      Page<Template> page = new PageImpl<>(List.of(template));
      when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
      when(mapper.toSummaryResponse(template)).thenReturn(summary);

      Page<TemplateSummaryResponse> result = service.findAll(new TemplateFilter());

      assertThat(result.getContent()).containsExactly(summary);
    }
  }

  @Nested
  @DisplayName("findByChannelAndName")
  class FindByChannelAndName {

    @Test
    @DisplayName("should return template when active template found")
    void shouldReturnTemplate() {
      Template template = TemplateFixtures.activeEmail("EMAIL_WELCOME");
      when(repository.findByChannelAndNameAndIsActive(ChannelType.EMAIL, "EMAIL_WELCOME", true))
          .thenReturn(Optional.of(template));

      Template result = service.findByChannelAndName(ChannelType.EMAIL, "EMAIL_WELCOME");

      assertThat(result).isEqualTo(template);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(repository.findByChannelAndNameAndIsActive(ChannelType.EMAIL, "EMAIL_WELCOME", true))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findByChannelAndName(ChannelType.EMAIL, "EMAIL_WELCOME"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should return mapped response when template found")
    void shouldReturnMappedResponse() {
      Template template = TemplateFixtures.activeEmail("EMAIL_WELCOME");
      when(repository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
      TemplateResponse response = stubResponse(template);

      assertThat(service.findById(TEMPLATE_ID)).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findById(TEMPLATE_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // -------------------------------------------------------------------------
  // update
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("should update template content and return response")
    void shouldUpdateTemplate() {
      Template template = TemplateFixtures.activeEmail("EMAIL_WELCOME");
      UpdateTemplateRequest request =
          new UpdateTemplateRequest("Updated body", "Updated subject", List.of());

      when(repository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
      stubAuditSupplier();
      when(repository.save(template)).thenReturn(template);
      TemplateResponse response = stubResponse(template);

      TemplateResponse result = service.update(TEMPLATE_ID, request);

      assertThat(template.getBodyTemplate()).isEqualTo("Updated body");
      assertThat(result).isEqualTo(response);
      verify(repository).save(template);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when template not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.update(
                      TEMPLATE_ID, new UpdateTemplateRequest("Updated body", null, List.of())))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
