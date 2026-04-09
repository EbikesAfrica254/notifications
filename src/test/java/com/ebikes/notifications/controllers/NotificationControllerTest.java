package com.ebikes.notifications.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.notifications.services.deliveries.DeliveryService;
import com.ebikes.notifications.services.notifications.NotificationService;
import com.ebikes.notifications.support.infrastructure.AbstractControllerTest;

@DisplayName("NotificationController")
@WebMvcTest(NotificationController.class)
class NotificationControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private NotificationService notificationService;
  @MockitoBean private DeliveryService deliveryService;

  @Nested
  @DisplayName("DELETE /notifications/{id}")
  class Cancel {

    @Test
    @DisplayName("should return 204 when notification is cancelled")
    void shouldReturn204WhenCancelled() throws Exception {
      doNothing().when(notificationService).cancel(any());

      mockMvc
          .perform(delete("/notifications/{id}", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isNoContent());

      verify(notificationService).cancel(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(delete("/notifications/{id}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /notifications/{id}")
  class FindById {

    @Test
    @DisplayName("should return 200 with notification")
    void shouldReturn200WithNotification() throws Exception {
      when(notificationService.findById(any())).thenReturn(null);

      mockMvc
          .perform(get("/notifications/{id}", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(notificationService).findById(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/notifications/{id}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /notifications/{notificationId}/deliveries")
  class FindDeliveries {

    @Test
    @DisplayName("should return 200 with delivery list")
    void shouldReturn200WithDeliveries() throws Exception {
      when(deliveryService.findByNotificationId(any())).thenReturn(List.of());

      mockMvc
          .perform(
              get("/notifications/{notificationId}/deliveries", UUID.randomUUID())
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(deliveryService).findByNotificationId(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              get("/notifications/{notificationId}/deliveries", UUID.randomUUID())
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /notifications")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      when(notificationService.search(any())).thenReturn(Page.empty());

      mockMvc.perform(get("/notifications").with(authenticatedJwt())).andExpect(status().isOk());

      verify(notificationService).search(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get("/notifications").with(anonymous())).andExpect(status().isUnauthorized());
    }
  }
}
