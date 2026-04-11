package com.ebikes.notifications.services.templates.variables;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ebikes.notifications.database.models.TemplateVariable;
import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;
import com.ebikes.notifications.services.templates.variables.resolvers.VariableResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VariableEnricher {

  private final List<VariableResolver> resolvers;

  public Map<String, Serializable> enrich(
      NotificationRequest request, List<TemplateVariable> variableDefinitions) {

    log.debug(
        "Enriching template variables: serviceReference={} organizationId={} subjectUserId={}",
        request.serviceReference(),
        request.organizationId(),
        request.subjectUserId());

    Map<String, Serializable> sealed = buildSealedMap(request);
    Map<String, Serializable> caller =
        filterCallerVariables(request, variableDefinitions, sealed.keySet());

    Map<String, Serializable> merged = new HashMap<>(sealed);
    merged.putAll(caller);

    log.debug("Final enriched variable keys: {}", merged.keySet());

    return Map.copyOf(merged);
  }

  private Map<String, Serializable> buildSealedMap(NotificationRequest request) {
    Map<String, Serializable> sealed = new HashMap<>();

    for (VariableResolver resolver : resolvers) {
      Map<String, Serializable> resolved = resolver.resolve(request);
      resolved.forEach(
          (key, value) -> {
            if (sealed.containsKey(key)) {
              log.warn(
                  "VariableResolver collision detected - key already sealed, ignoring: key={}"
                      + " resolver={}",
                  key,
                  resolver.getClass().getSimpleName());
            } else {
              sealed.put(key, value);
            }
          });
    }

    log.debug("Sealed resolver variable keys: {}", sealed.keySet());
    return sealed;
  }

  private Map<String, Serializable> filterCallerVariables(
      NotificationRequest request,
      List<TemplateVariable> variableDefinitions,
      Set<String> sealedKeys) {

    if (request.variables() == null || request.variables().isEmpty()) {
      return Map.of();
    }

    Set<String> declaredNames =
        variableDefinitions.stream().map(TemplateVariable::name).collect(Collectors.toSet());

    Map<String, Serializable> filtered = new HashMap<>();

    request
        .variables()
        .forEach(
            (key, value) -> {
              if (sealedKeys.contains(key)) {
                log.warn(
                    "Caller variable collides with sealed resolver variable - rejected: key={}",
                    key);
              } else if (!declaredNames.contains(key)) {
                log.debug(
                    "Caller variable not declared in template definitions - rejected: key={}", key);
              } else {
                filtered.put(key, value);
              }
            });

    log.debug("Accepted caller variable keys: {}", filtered.keySet());
    return filtered;
  }
}
