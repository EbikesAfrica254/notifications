package com.ebikes.notifications.dtos.requests.templates;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.notifications.database.models.TemplateVariable;

public record UpdateTemplateRequest(
    @NotBlank String bodyTemplate,
    @Size(max = 500) String subject,
    @NotNull @Valid List<TemplateVariable> variableDefinitions)
    implements Serializable {

  public UpdateTemplateRequest {
    variableDefinitions = List.copyOf(variableDefinitions);
  }
}
