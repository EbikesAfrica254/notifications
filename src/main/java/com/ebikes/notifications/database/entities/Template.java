package com.ebikes.notifications.database.entities;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.Type;

import com.ebikes.notifications.database.entities.bases.AuditableEntity;
import com.ebikes.notifications.database.models.TemplateVariable;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ContentType;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "templates",
    schema = "notifications",
    indexes = {
      @Index(name = "idx_templates_lookup", columnList = "name, is_active"),
      @Index(name = "idx_templates_channel", columnList = "channel, is_active")
    })
public class Template extends AuditableEntity {

  @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
  @NotBlank(message = "Body template is required") private String bodyTemplate;

  @Column(name = "channel", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull(message = "Channel is required") private ChannelType channel;

  @Column(name = "content_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull(message = "Content type is required") private ContentType contentType;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "name", nullable = false, unique = true, length = 100)
  @NotBlank @Size(min = 3, max = 100) @Pattern(
      regexp = "^[A-Z][A-Z0-9_]{2,99}$",
      message = "Name must be SCREAMING_SNAKE_CASE and begin with an uppercase letter")
  private String name;

  @Column(name = "subject", length = 500)
  @Size(max = 500, message = "Subject must not exceed 500 characters") private String subject;

  @Column(name = "variable_definitions", nullable = false, columnDefinition = "JSONB")
  @Type(JsonBinaryType.class)
  @NotNull(message = "Variable definitions are required") private List<TemplateVariable> variableDefinitions;

  @Column(name = "version", nullable = false)
  @Version
  private int version;

  @Builder
  public Template(
      String bodyTemplate,
      ChannelType channel,
      ContentType contentType,
      String name,
      String subject,
      List<TemplateVariable> variableDefinitions) {
    this.bodyTemplate = bodyTemplate;
    this.channel = channel;
    this.contentType = contentType;
    this.isActive = true;
    this.name = name;
    this.subject = subject;
    this.variableDefinitions =
        variableDefinitions != null ? List.copyOf(variableDefinitions) : List.of();
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }

  public void updateContent(
      String newBodyTemplate, String newSubject, List<TemplateVariable> newVariableDefinitions) {
    if (newBodyTemplate == null || newBodyTemplate.isBlank()) {
      throw new IllegalArgumentException("Body template is required");
    }
    this.bodyTemplate = newBodyTemplate;
    this.subject = newSubject;
    this.variableDefinitions =
        newVariableDefinitions != null ? List.copyOf(newVariableDefinitions) : List.of();
  }

  @AssertTrue(message = "HTML content type is only permitted for EMAIL channel") private boolean isContentTypeValidForChannel() {
    if (channel == null || contentType == null) {
      return true;
    }
    return contentType != ContentType.HTML || channel == ChannelType.EMAIL;
  }

  @AssertTrue(
      message = "Subject is required for EMAIL templates and must be null for all other channels")
  private boolean isSubjectValidForChannel() {
    if (channel == null) {
      return true;
    }
    if (channel == ChannelType.EMAIL) {
      return subject != null && !subject.isBlank();
    }
    return subject == null;
  }

  @AssertTrue(message = "Body template exceeds maximum length for the configured channel") private boolean isBodyLengthValidForChannel() {
    if (channel == null || bodyTemplate == null) {
      return true;
    }
    return switch (channel) {
      case SMS -> bodyTemplate.length() <= 320;
      case SSE -> bodyTemplate.length() <= 4_096;
      case EMAIL -> bodyTemplate.length() <= 102_400;
      case WHATSAPP -> bodyTemplate.length() <= 1_024;
    };
  }
}
