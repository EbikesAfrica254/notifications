package com.ebikes.notifications.database.models;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.ebikes.notifications.enums.VariableType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TemplateVariable(
    @NotBlank(message = "Variable name is required") @Pattern(
            regexp = "^[a-z][a-zA-Z0-9]{1,63}$",
            message =
                "Variable name must be camelCase, 2-64 characters, starting with a lowercase"
                    + " letter")
        String name,
    @NotNull(message = "Variable type is required") VariableType type,
    @NotBlank(message = "Variable description is required") @jakarta.validation.constraints.Size(
            max = 255,
            message = "Description must not exceed 255 characters")
        String description,
    boolean required,
    boolean sensitive)
    implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @JsonCreator
  public TemplateVariable(
      @JsonProperty("name") String name,
      @JsonProperty("type") VariableType type,
      @JsonProperty("description") String description,
      @JsonProperty("required") boolean required,
      @JsonProperty("sensitive") boolean sensitive) {

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Variable name is required");
    }
    if (type == null) {
      throw new IllegalArgumentException("Variable type is required");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Variable description is required");
    }

    this.name = name;
    this.type = type;
    this.description = description;
    this.required = required;
    this.sensitive = sensitive;
  }
}
