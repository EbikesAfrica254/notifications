package com.ebikes.notifications.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.notifications.dtos.requests.filters.ChannelPreferenceFilter;
import com.ebikes.notifications.dtos.requests.preferences.organization.CreateOrganizationPreferenceRequest;
import com.ebikes.notifications.dtos.requests.preferences.organization.UpdateOrganizationPreferenceRequest;
import com.ebikes.notifications.dtos.responses.api.PaginatedResponse;
import com.ebikes.notifications.dtos.responses.api.SuccessResponse;
import com.ebikes.notifications.dtos.responses.preferences.organization.OrganizationPreferenceResponse;
import com.ebikes.notifications.services.preferences.OrganizationPreferenceService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/organizations/preferences")
@RestController
@Validated
public class OrganizationPreferenceController {

  private final OrganizationPreferenceService organizationPreferenceService;

  @PostMapping
  public ResponseEntity<SuccessResponse<OrganizationPreferenceResponse>> create(
      @Valid @RequestBody CreateOrganizationPreferenceRequest request) {
    OrganizationPreferenceResponse preference = organizationPreferenceService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(preference, "Organization preference created successfully"));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    organizationPreferenceService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<OrganizationPreferenceResponse>> findById(
      @PathVariable UUID id) {
    OrganizationPreferenceResponse preference = organizationPreferenceService.findById(id);
    return ResponseEntity.ok(SuccessResponse.of(preference));
  }

  @GetMapping
  public ResponseEntity<PaginatedResponse<OrganizationPreferenceResponse>> search(
      @Valid @ModelAttribute ChannelPreferenceFilter filter) {
    Page<OrganizationPreferenceResponse> page = organizationPreferenceService.search(filter);
    return ResponseEntity.ok(
        PaginatedResponse.from("Organization preferences retrieved successfully.", page));
  }

  @PutMapping("/{id}")
  public ResponseEntity<SuccessResponse<OrganizationPreferenceResponse>> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateOrganizationPreferenceRequest request) {
    OrganizationPreferenceResponse preference = organizationPreferenceService.update(id, request);
    return ResponseEntity.ok(
        SuccessResponse.of(preference, "Organization preference updated successfully"));
  }
}
