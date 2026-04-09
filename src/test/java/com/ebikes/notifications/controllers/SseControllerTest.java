package com.ebikes.notifications.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ebikes.notifications.adapters.channels.sse.SseConnectionManager;
import com.ebikes.notifications.support.fixtures.SecurityFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractControllerTest;

@DisplayName("SseController")
@WebMvcTest(SseController.class)
class SseControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SseConnectionManager connectionManager;

  @Nested
  @DisplayName("GET /sse/stream")
  class StreamNotifications {

    @Test
    @DisplayName("should return 200 and register connection for authenticated user")
    void shouldReturn200AndRegisterConnection() throws Exception {
      when(connectionManager.createConnection(eq(SecurityFixtures.TEST_USER_ID)))
          .thenReturn(new SseEmitter());

      mockMvc.perform(get("/sse/stream").with(authenticatedJwt())).andExpect(status().isOk());

      verify(connectionManager).createConnection(eq(SecurityFixtures.TEST_USER_ID));
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get("/sse/stream").with(anonymous())).andExpect(status().isUnauthorized());
    }
  }
}
