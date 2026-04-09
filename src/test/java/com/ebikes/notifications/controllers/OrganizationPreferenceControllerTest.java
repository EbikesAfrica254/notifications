package com.ebikes.notifications.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.notifications.services.preferences.OrganizationPreferenceService;
import com.ebikes.notifications.support.infrastructure.AbstractControllerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("OrganizationPreferenceController")
@WebMvcTest(OrganizationPreferenceController.class)
class OrganizationPreferenceControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private OrganizationPreferenceService organizationPreferenceService;

  private static final String BASE_URL = "/organizations/preferences";

  @Nested
  @DisplayName("POST /organizations/preferences")
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
      when(organizationPreferenceService.create(any())).thenReturn(null);

      mockMvc
          .perform(
              post(BASE_URL)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_BODY))
          .andExpect(status().isCreated());

      verify(organizationPreferenceService).create(any());
    }

    @Test
    @DisplayName("should return 400 when required fields are missing")
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
      mockMvc
          .perform(
              post(BASE_URL)
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
              post(BASE_URL)
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_BODY))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("DELETE /organizations/preferences/{id}")
  class Delete {

    @Test
    @DisplayName("should return 204 when preference is deleted")
    void shouldReturn204WhenDeleted() throws Exception {
      doNothing().when(organizationPreferenceService).delete(any());

      mockMvc
          .perform(delete(BASE_URL + "/{id}", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isNoContent());

      verify(organizationPreferenceService).delete(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(delete(BASE_URL + "/{id}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /organizations/preferences/{id}")
  class FindById {

    @Test
    @DisplayName("should return 200 with preference")
    void shouldReturn200WithPreference() throws Exception {
      when(organizationPreferenceService.findById(any())).thenReturn(null);

      mockMvc
          .perform(get(BASE_URL + "/{id}", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(organizationPreferenceService).findById(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get(BASE_URL + "/{id}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /organizations/preferences")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      when(organizationPreferenceService.search(any())).thenReturn(Page.empty());

      mockMvc.perform(get(BASE_URL).with(authenticatedJwt())).andExpect(status().isOk());

      verify(organizationPreferenceService).search(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get(BASE_URL).with(anonymous())).andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /organizations/preferences/{id}")
  class Update {

    private static final String VALID_BODY =
        """
        { "enabled": false }
        """;

    @Test
    @DisplayName("should return 200 when preference is updated")
    void shouldReturn200WhenUpdated() throws Exception {
      when(organizationPreferenceService.update(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              put(BASE_URL + "/{id}", UUID.randomUUID())
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_BODY))
          .andExpect(status().isOk());

      verify(organizationPreferenceService).update(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put(BASE_URL + "/{id}", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_BODY))
          .andExpect(status().isUnauthorized());
    }
  }
}
