package com.ebikes.notifications.services.templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ebikes.notifications.database.models.TemplateVariable;
import com.ebikes.notifications.enums.TemplateContentType;
import com.ebikes.notifications.enums.VariableType;
import com.ebikes.notifications.exceptions.TemplateException;

@DisplayName("TemplateProcessor")
class TemplateProcessorTest {

  private TemplateProcessor processor;

  private static TemplateVariable required(String name) {
    return new TemplateVariable(name, VariableType.STRING, "A required variable", true, false);
  }

  private static TemplateVariable optional(String name) {
    return new TemplateVariable(name, VariableType.STRING, "An optional variable", false, false);
  }

  private static TemplateVariable sensitive(String name) {
    return new TemplateVariable(name, VariableType.STRING, "A sensitive variable", false, true);
  }

  @BeforeEach
  void setUp() {
    processor = new TemplateProcessor();
  }

  // -------------------------------------------------------------------------
  // render
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("render")
  class Render {

    @Test
    @DisplayName("should render HTML template with supplied variable")
    void shouldRenderHtmlTemplate() {
      String template = "<p>Hello [[${name}]]</p>";
      List<TemplateVariable> definitions = List.of(required("name"));
      Map<String, Serializable> variables = Map.of("name", "Alice");

      String result = processor.render(TemplateContentType.HTML, template, definitions, variables);

      assertThat(result).contains("Hello Alice");
    }

    @Test
    @DisplayName("should render PLAIN_TEXT template with supplied variable")
    void shouldRenderPlainTextTemplate() {
      String template = "Hello [[${name}]]";
      List<TemplateVariable> definitions = List.of(required("name"));
      Map<String, Serializable> variables = Map.of("name", "Bob");

      String result =
          processor.render(TemplateContentType.PLAIN_TEXT, template, definitions, variables);

      assertThat(result).contains("Hello Bob");
    }

    @Test
    @DisplayName("should throw TemplateException when template content is null")
    void shouldThrowOnNullTemplateContent() {
      assertThatThrownBy(
              () -> processor.render(TemplateContentType.PLAIN_TEXT, null, List.of(), Map.of()))
          .isInstanceOf(TemplateException.class);
    }

    @Test
    @DisplayName("should throw TemplateException when template content is blank")
    void shouldThrowOnBlankTemplateContent() {
      assertThatThrownBy(
              () -> processor.render(TemplateContentType.PLAIN_TEXT, "   ", List.of(), Map.of()))
          .isInstanceOf(TemplateException.class);
    }

    @Test
    @DisplayName("should throw TemplateException when a required variable is missing")
    void shouldThrowOnMissingRequiredVariable() {
      String template = "Hello [[${name}]]";
      List<TemplateVariable> definitions = List.of(required("name"));

      assertThatThrownBy(
              () ->
                  processor.render(TemplateContentType.PLAIN_TEXT, template, definitions, Map.of()))
          .isInstanceOf(TemplateException.class)
          .hasMessageContaining("Missing required template variables");
    }

    @Test
    @DisplayName(
        "should throw TemplateException when rendered output is empty due to unresolved expression")
    void shouldThrowWhenRenderedOutputIsEmpty() {
      // Thymeleaf TEXT mode silently drops unresolved [[${...}]] to empty string.
      // A template consisting only of an undeclared variable produces blank output,
      // which triggers the empty-content guard.
      String template = "[[${undeclared}]]";
      List<TemplateVariable> definitions = List.of(optional("other"));
      Map<String, Serializable> variables = Map.of("other", "value");

      assertThatThrownBy(
              () ->
                  processor.render(
                      TemplateContentType.PLAIN_TEXT, template, definitions, variables))
          .isInstanceOf(TemplateException.class)
          .hasMessageContaining("empty content");
    }

    @Test
    @DisplayName("should handle null variable definitions as empty list")
    void shouldHandleNullDefinitions() {
      String template = "Static content";

      String result = processor.render(TemplateContentType.PLAIN_TEXT, template, null, Map.of());

      assertThat(result).isEqualTo("Static content");
    }

    @Test
    @DisplayName("should render static content alongside declared variable, ignoring undeclared")
    void shouldIgnoreUndeclaredVariables() {
      // Thymeleaf drops undeclared [[${...}]] silently; only declared variables are injected.
      String template = "Hello [[${name}]] extra [[${undeclared}]]";
      List<TemplateVariable> definitions = List.of(required("name"));
      Map<String, Serializable> variables = Map.of("name", "Alice");

      // "undeclared" is dropped; result contains "Hello Alice extra " with trailing whitespace
      // which is non-blank, so no exception is thrown
      String result =
          processor.render(TemplateContentType.PLAIN_TEXT, template, definitions, variables);

      assertThat(result).contains("Hello Alice");
      assertThat(result).doesNotContain("undeclared");
    }
  }

  // -------------------------------------------------------------------------
  // redactVariables
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("redactVariables")
  class RedactVariables {

    @Test
    @DisplayName("should return null when variables map is null")
    void shouldReturnNullWhenVariablesNull() {
      assertThat(processor.redactVariables(null, List.of(sensitive("token")))).isNull();
    }

    @Test
    @DisplayName("should return empty map when variables map is empty")
    void shouldReturnEmptyWhenVariablesEmpty() {
      Map<String, Serializable> result =
          processor.redactVariables(Map.of(), List.of(sensitive("token")));
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return variables unchanged when definitions are null")
    void shouldReturnUnchangedWhenDefinitionsNull() {
      Map<String, Serializable> variables = Map.of("name", "Alice");

      assertThat(processor.redactVariables(variables, null)).isEqualTo(variables);
    }

    @Test
    @DisplayName("should return variables unchanged when definitions are empty")
    void shouldReturnUnchangedWhenDefinitionsEmpty() {
      Map<String, Serializable> variables = Map.of("name", "Alice");

      assertThat(processor.redactVariables(variables, List.of())).isEqualTo(variables);
    }

    @Test
    @DisplayName("should return variables unchanged when no definitions are sensitive")
    void shouldReturnUnchangedWhenNoSensitiveDefinitions() {
      Map<String, Serializable> variables = Map.of("name", "Alice", "code", "12345");
      List<TemplateVariable> definitions = List.of(required("name"), optional("code"));

      assertThat(processor.redactVariables(variables, definitions)).isEqualTo(variables);
    }

    @Test
    @DisplayName("should redact sensitive variable values")
    void shouldRedactSensitiveVariables() {
      Map<String, Serializable> variables = Map.of("token", "secret-value");
      List<TemplateVariable> definitions = List.of(sensitive("token"));

      Map<String, Serializable> result = processor.redactVariables(variables, definitions);

      assertThat(result).containsEntry("token", "••••••");
    }

    @Test
    @DisplayName("should redact sensitive variables and preserve non-sensitive ones")
    void shouldPreserveNonSensitiveVariables() {
      Map<String, Serializable> variables =
          Map.of(
              "name", "Alice",
              "token", "secret-value");
      List<TemplateVariable> definitions = List.of(required("name"), sensitive("token"));

      Map<String, Serializable> result = processor.redactVariables(variables, definitions);

      assertThat(result).containsEntry("name", "Alice").containsEntry("token", "••••••");
    }
  }
}
