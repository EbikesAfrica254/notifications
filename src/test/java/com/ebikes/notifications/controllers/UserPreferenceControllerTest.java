package com.ebikes.notifications.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.services.preferences.UserPreferenceService;
import com.ebikes.notifications.support.fixtures.SecurityFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractControllerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("UserPreferenceController")
@WebMvcTest(UserPreferenceController.class)
class UserPreferenceControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserPreferenceService userPreferenceService;

  private static final String BASE_URL = "/users/{userId}/preferences";
  private static final String USER_ID = SecurityFixtures.TEST_USER_ID;
  private static final ChannelType CHANNEL = ChannelType.EMAIL;
  private static final NotificationCategory CATEGORY = NotificationCategory.TRANSACTIONAL;

  @Nested
  @DisplayName("POST /users/{userId}/preferences")
  class Create {

    private static final String VALID_BODY =
        """
        {
          "channel": "EMAIL",
          "category": "TRANSACTIONAL"
        }
        """;

    @Test
    @DisplayName("should return 201 when preference is created")
    void shouldReturn201WhenCreated() throws Exception {
      when(userPreferenceService.create(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              post(BASE_URL, USER_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_BODY))
          .andExpect(status().isCreated());

      verify(userPreferenceService).create(any(), any());
    }

    @Test
    @DisplayName("should return 400 when required fields are missing")
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
      mockMvc
          .perform(
              post(BASE_URL, USER_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post(BASE_URL, USER_ID)
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_BODY))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("DELETE /users/{userId}/preferences")
  class Delete {

    @Test
    @DisplayName("should return 204 when preference is deleted")
    void shouldReturn204WhenDeleted() throws Exception {
      doNothing().when(userPreferenceService).delete(any(), any(), any());

      mockMvc
          .perform(
              delete(BASE_URL, USER_ID)
                  .with(authenticatedJwt())
                  .param("channel", CHANNEL.name())
                  .param("category", CATEGORY.name()))
          .andExpect(status().isNoContent());

      verify(userPreferenceService).delete(eq(USER_ID), eq(CHANNEL), eq(CATEGORY));
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              delete(BASE_URL, USER_ID)
                  .with(anonymous())
                  .param("channel", CHANNEL.name())
                  .param("category", CATEGORY.name()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /users/{userId}/preferences/find")
  class FindByCompositeKey {

    @Test
    @DisplayName("should return 200 with preference")
    void shouldReturn200WithPreference() throws Exception {
      when(userPreferenceService.findByCompositeKeyResponse(any(), any(), any())).thenReturn(null);

      mockMvc
          .perform(
              get(BASE_URL + "/find", USER_ID)
                  .with(authenticatedJwt())
                  .param("channel", CHANNEL.name())
                  .param("category", CATEGORY.name()))
          .andExpect(status().isOk());

      verify(userPreferenceService)
          .findByCompositeKeyResponse(eq(USER_ID), eq(CHANNEL), eq(CATEGORY));
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              get(BASE_URL + "/find", USER_ID)
                  .with(anonymous())
                  .param("channel", CHANNEL.name())
                  .param("category", CATEGORY.name()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /users/{userId}/preferences")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      doNothing().when(userPreferenceService).existsByUserId(any());
      when(userPreferenceService.search(any())).thenReturn(Page.empty());

      mockMvc.perform(get(BASE_URL, USER_ID).with(authenticatedJwt())).andExpect(status().isOk());

      verify(userPreferenceService).existsByUserId(USER_ID);
      verify(userPreferenceService).search(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get(BASE_URL, USER_ID).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /users/{userId}/preferences")
  class Update {

    private static final String VALID_BODY =
        """
        { "enabled": false }
        """;

    @Test
    @DisplayName("should return 200 when preference is updated")
    void shouldReturn200WhenUpdated() throws Exception {
      when(userPreferenceService.update(any(), any(), any(), any())).thenReturn(null);

      mockMvc
          .perform(
              put(BASE_URL, USER_ID)
                  .with(authenticatedJwt())
                  .param("channel", CHANNEL.name())
                  .param("category", CATEGORY.name())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_BODY))
          .andExpect(status().isOk());

      verify(userPreferenceService).update(eq(USER_ID), eq(CHANNEL), eq(CATEGORY), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put(BASE_URL, USER_ID)
                  .with(anonymous())
                  .param("channel", CHANNEL.name())
                  .param("category", CATEGORY.name())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_BODY))
          .andExpect(status().isUnauthorized());
    }
  }
}
