package com.ebikes.notifications.dtos.requests.templates;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.notifications.database.models.TemplateVariable;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ContentType;

public record CreateTemplateRequest(
    @NotBlank String bodyTemplate,
    @NotNull ChannelType channel,
    @NotNull ContentType contentType,
    @NotBlank @Size(min = 3, max = 100) @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,99}$") String name,
    @Size(max = 500) String subject,
    @NotNull @Valid List<TemplateVariable> variableDefinitions)
    implements Serializable {

  public CreateTemplateRequest {
    variableDefinitions = List.copyOf(variableDefinitions);
  }
}
