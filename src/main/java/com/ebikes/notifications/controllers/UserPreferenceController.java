package com.ebikes.notifications.controllers;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.notifications.dtos.requests.filters.ChannelPreferenceFilter;
import com.ebikes.notifications.dtos.requests.preferences.user.CreateUserPreferenceRequest;
import com.ebikes.notifications.dtos.requests.preferences.user.UpdateUserPreferenceRequest;
import com.ebikes.notifications.dtos.responses.api.PaginatedResponse;
import com.ebikes.notifications.dtos.responses.api.SuccessResponse;
import com.ebikes.notifications.dtos.responses.preferences.user.UserPreferenceResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.services.preferences.UserPreferenceService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/users/{userId}/preferences")
@RestController
@Validated
public class UserPreferenceController {

  private final UserPreferenceService userPreferenceService;

  @PostMapping
  public ResponseEntity<SuccessResponse<UserPreferenceResponse>> create(
      @PathVariable String userId, @Valid @RequestBody CreateUserPreferenceRequest request) {
    UserPreferenceResponse preference = userPreferenceService.create(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(preference, "User preference created successfully"));
  }

  @DeleteMapping
  public ResponseEntity<Void> delete(
      @PathVariable String userId,
      @RequestParam ChannelType channel,
      @RequestParam NotificationCategory category) {
    userPreferenceService.delete(userId, channel, category);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/find")
  public ResponseEntity<SuccessResponse<UserPreferenceResponse>> findByCompositeKey(
      @PathVariable String userId,
      @RequestParam ChannelType channel,
      @RequestParam NotificationCategory category) {
    UserPreferenceResponse preference =
        userPreferenceService.findByCompositeKeyResponse(userId, channel, category);
    return ResponseEntity.ok(SuccessResponse.of(preference));
  }

  @GetMapping
  public ResponseEntity<PaginatedResponse<UserPreferenceResponse>> search(
      @Valid @ModelAttribute ChannelPreferenceFilter filter, @PathVariable String userId) {
    userPreferenceService.existsByUserId(userId);
    Page<UserPreferenceResponse> page = userPreferenceService.search(filter);
    return ResponseEntity.ok(
        PaginatedResponse.from("User preferences retrieved successfully.", page));
  }

  @PutMapping
  public ResponseEntity<SuccessResponse<UserPreferenceResponse>> update(
      @PathVariable String userId,
      @RequestParam ChannelType channel,
      @RequestParam NotificationCategory category,
      @Valid @RequestBody UpdateUserPreferenceRequest request) {
    UserPreferenceResponse preference =
        userPreferenceService.update(userId, channel, category, request);
    return ResponseEntity.ok(
        SuccessResponse.of(preference, "User preference updated successfully"));
  }
}
