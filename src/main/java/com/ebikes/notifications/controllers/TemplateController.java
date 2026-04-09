package com.ebikes.notifications.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.notifications.dtos.requests.filters.TemplateFilter;
import com.ebikes.notifications.dtos.requests.templates.CreateTemplateRequest;
import com.ebikes.notifications.dtos.requests.templates.UpdateTemplateRequest;
import com.ebikes.notifications.dtos.responses.api.PaginatedResponse;
import com.ebikes.notifications.dtos.responses.api.SuccessResponse;
import com.ebikes.notifications.dtos.responses.templates.TemplateResponse;
import com.ebikes.notifications.dtos.responses.templates.TemplateSummaryResponse;
import com.ebikes.notifications.services.templates.TemplateService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/templates")
@RestController
public class TemplateController {

  private static final Logger log = LoggerFactory.getLogger(TemplateController.class);
  private final TemplateService templateService;

  @PutMapping("/{id}/activate")
  public ResponseEntity<SuccessResponse<TemplateResponse>> activate(@PathVariable UUID id) {
    TemplateResponse template = templateService.activate(id);
    return ResponseEntity.ok(SuccessResponse.of(template, "Template activated successfully"));
  }

  @PostMapping
  public ResponseEntity<SuccessResponse<TemplateResponse>> create(
      @Valid @RequestBody CreateTemplateRequest request) {
    TemplateResponse template = templateService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(template, "Template created successfully"));
  }

  @PutMapping("/{id}/deactivate")
  public ResponseEntity<SuccessResponse<TemplateResponse>> deactivate(@PathVariable UUID id) {
    TemplateResponse template = templateService.deactivate(id);
    return ResponseEntity.ok(SuccessResponse.of(template, "Template deactivated successfully"));
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<TemplateResponse>> findById(@PathVariable UUID id) {
    TemplateResponse template = templateService.findById(id);
    return ResponseEntity.ok(SuccessResponse.of(template));
  }

  @GetMapping
  public ResponseEntity<PaginatedResponse<TemplateSummaryResponse>> search(
      @Valid @ModelAttribute TemplateFilter filter) {
    log.info("Searching templates with filter: {}", filter);
    Page<TemplateSummaryResponse> page = templateService.findAll(filter);
    return ResponseEntity.ok(PaginatedResponse.from("Templates retrieved successfully.", page));
  }

  @PutMapping("/{id}")
  public ResponseEntity<SuccessResponse<TemplateResponse>> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateTemplateRequest request) {
    TemplateResponse template = templateService.update(id, request);
    return ResponseEntity.ok(SuccessResponse.of(template, "Template updated successfully"));
  }
}
