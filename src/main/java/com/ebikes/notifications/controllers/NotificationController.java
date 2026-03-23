package com.ebikes.notifications.controllers;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.notifications.dtos.requests.filters.NotificationFilter;
import com.ebikes.notifications.dtos.responses.api.PaginatedResponse;
import com.ebikes.notifications.dtos.responses.api.SuccessResponse;
import com.ebikes.notifications.dtos.responses.deliveries.DeliveryResponse;
import com.ebikes.notifications.dtos.responses.notifications.NotificationResponse;
import com.ebikes.notifications.dtos.responses.notifications.NotificationSummaryResponse;
import com.ebikes.notifications.services.deliveries.DeliveryService;
import com.ebikes.notifications.services.notifications.NotificationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/notifications")
@RestController
@Validated
public class NotificationController {

  private final DeliveryService deliveryService;
  private final NotificationService notificationService;

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancel(@PathVariable UUID id) {
    notificationService.cancel(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<NotificationResponse>> findById(@PathVariable UUID id) {
    NotificationResponse notification = notificationService.findById(id);
    return ResponseEntity.ok(SuccessResponse.of(notification));
  }

  @GetMapping("/{notificationId}/deliveries")
  public ResponseEntity<SuccessResponse<List<DeliveryResponse>>> findDeliveries(
      @PathVariable UUID notificationId) {
    List<DeliveryResponse> deliveries = deliveryService.findByNotificationId(notificationId);
    return ResponseEntity.ok(SuccessResponse.of(deliveries));
  }

  @GetMapping
  public ResponseEntity<PaginatedResponse<NotificationSummaryResponse>> search(
      @Valid @ModelAttribute NotificationFilter filter) {
    Page<NotificationSummaryResponse> page = notificationService.search(filter);
    return ResponseEntity.ok(PaginatedResponse.from("Notifications retrieved successfully.", page));
  }
}
