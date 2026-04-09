package com.ebikes.notifications.services.templates;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.ebikes.notifications.database.models.TemplateVariable;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.enums.TemplateContentType;
import com.ebikes.notifications.exceptions.TemplateException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TemplateProcessor {

  private static final Set<String> UNSUBSTITUTED_PATTERNS = Set.of("[[${", "{{");

  private final Map<TemplateMode, TemplateEngine> templateEngines;

  public TemplateProcessor() {
    this.templateEngines = new EnumMap<>(TemplateMode.class);
    initializeTemplateEngines();
  }

  public Map<String, Serializable> redactVariables(
      Map<String, Serializable> variables, List<TemplateVariable> variableDefinitions) {

    if (variables == null || variables.isEmpty()) {
      return variables;
    }

    if (variableDefinitions == null || variableDefinitions.isEmpty()) {
      return variables;
    }

    Set<String> sensitiveNames =
        variableDefinitions.stream()
            .filter(TemplateVariable::sensitive)
            .map(TemplateVariable::name)
            .collect(Collectors.toSet());

    if (sensitiveNames.isEmpty()) {
      return variables;
    }

    return variables.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> sensitiveNames.contains(entry.getKey()) ? "••••••" : entry.getValue()));
  }

  public String render(
      TemplateContentType templateContentType,
      String templateContent,
      List<TemplateVariable> variableDefinitions,
      Map<String, Serializable> variables) {

    try {
      validateTemplateContent(templateContent);

      List<TemplateVariable> definitions =
          variableDefinitions != null ? variableDefinitions : List.of();

      validateRequiredVariables(definitions, variables);

      TemplateMode templateMode = mapContentTypeToTemplateMode(templateContentType);

      TemplateEngine engine = templateEngines.get(templateMode);
      if (engine == null) {
        throw new TemplateException(
            ResponseCode.TEMPLATE_PROCESSING_ERROR,
            "No template engine configured for mode: " + templateMode);
      }

      Context context = buildSafeContext(definitions, variables);

      String result = engine.process(templateContent, context);

      validateProcessingResult(result, templateContent);

      log.debug(
          "Template processing completed - mode={} templateContentType={} originalLength={}"
              + " processedLength={}",
          templateMode,
          templateContentType,
          templateContent.length(),
          result.length());

      return result;

    } catch (TemplateException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Unexpected error during template processing - templateContentType={}",
          templateContentType,
          e);
      throw new TemplateException(
          ResponseCode.TEMPLATE_PROCESSING_ERROR,
          "Failed to process template: " + e.getMessage(),
          e);
    }
  }

  private Context buildSafeContext(
      List<TemplateVariable> definitions, Map<String, Serializable> variables) {

    Context context = new Context();

    if (variables == null || variables.isEmpty() || definitions.isEmpty()) {
      return context;
    }

    Map<String, TemplateVariable> definitionsByName =
        definitions.stream().collect(Collectors.toMap(TemplateVariable::name, Function.identity()));

    variables.entrySet().stream()
        .filter(entry -> definitionsByName.containsKey(entry.getKey()))
        .forEach(entry -> context.setVariable(entry.getKey(), entry.getValue()));

    return context;
  }

  private TemplateEngine createTemplateEngine(TemplateMode templateMode) {
    TemplateEngine engine = new TemplateEngine();

    StringTemplateResolver resolver = new StringTemplateResolver();
    resolver.setTemplateMode(templateMode);
    resolver.setCacheable(true);

    engine.setTemplateResolver(resolver);

    log.debug("Created TemplateEngine for mode: {}", templateMode);

    return engine;
  }

  private void initializeTemplateEngines() {
    templateEngines.put(TemplateMode.HTML, createTemplateEngine(TemplateMode.HTML));
    templateEngines.put(TemplateMode.TEXT, createTemplateEngine(TemplateMode.TEXT));

    log.info(
        "Initialized {} template engines for modes: {}",
        templateEngines.size(),
        templateEngines.keySet());
  }

  private TemplateMode mapContentTypeToTemplateMode(TemplateContentType templateContentType) {
    return switch (templateContentType) {
      case HTML -> TemplateMode.HTML;
      case JSON, PLAIN_TEXT -> TemplateMode.TEXT;
    };
  }

  private void validateRequiredVariables(
      List<TemplateVariable> definitions, Map<String, Serializable> variables) {

    Set<String> provided = variables != null ? variables.keySet() : Set.of();

    List<String> missingRequired =
        definitions.stream()
            .filter(TemplateVariable::required)
            .map(TemplateVariable::name)
            .filter(name -> !provided.contains(name))
            .toList();

    if (!missingRequired.isEmpty()) {
      throw new TemplateException(
          ResponseCode.TEMPLATE_PROCESSING_ERROR,
          "Missing required template variables: " + missingRequired);
    }
  }

  private void validateProcessingResult(String result, String originalContent) {
    if (result == null || result.isBlank()) {
      log.error(
          "Template processing resulted in empty content. Original length: {}",
          originalContent.length());
      throw new TemplateException(
          ResponseCode.TEMPLATE_PROCESSING_ERROR, "Template processing resulted in empty content");
    }

    UNSUBSTITUTED_PATTERNS.stream()
        .filter(result::contains)
        .findFirst()
        .ifPresent(
            pattern -> {
              throw new TemplateException(
                  ResponseCode.TEMPLATE_PROCESSING_ERROR,
                  "Rendered output contains unsubstituted placeholder pattern '"
                      + pattern
                      + "' - verify template syntax uses Thymeleaf inline expressions"
                      + " [[${variable}]]");
            });
  }

  private void validateTemplateContent(String templateContent) {
    if (templateContent == null || templateContent.isBlank()) {
      throw new TemplateException(
          ResponseCode.TEMPLATE_PROCESSING_ERROR, "Template content cannot be null or empty");
    }
  }
}
