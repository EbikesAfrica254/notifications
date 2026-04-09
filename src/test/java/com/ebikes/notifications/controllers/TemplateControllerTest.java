package com.ebikes.notifications.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import com.ebikes.notifications.services.templates.TemplateService;
import com.ebikes.notifications.support.fixtures.TemplateRequestFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractControllerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("TemplateController")
@WebMvcTest(TemplateController.class)
class TemplateControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TemplateService templateService;

  @Nested
  @DisplayName("PUT /templates/{id}/activate")
  class Activate {

    @Test
    @DisplayName("should return 200 when template is activated")
    void shouldReturn200WhenActivated() throws Exception {
      when(templateService.activate(any())).thenReturn(null);

      mockMvc
          .perform(put("/templates/{id}/activate", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(templateService).activate(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(put("/templates/{id}/activate", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /templates")
  class Create {

    @Test
    @DisplayName("should return 201 when template is created")
    void shouldReturn201WhenCreated() throws Exception {
      when(templateService.create(any())).thenReturn(null);

      mockMvc
          .perform(
              post("/templates")
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          TemplateRequestFixtures.createEmailRequest("EMAIL_WELCOME"))))
          .andExpect(status().isCreated());

      verify(templateService).create(any());
    }

    @Test
    @DisplayName("should return 400 when required fields are missing")
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
      mockMvc
          .perform(
              post("/templates")
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
              post("/templates")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          TemplateRequestFixtures.createEmailRequest("EMAIL_WELCOME"))))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /templates/{id}/deactivate")
  class Deactivate {

    @Test
    @DisplayName("should return 200 when template is deactivated")
    void shouldReturn200WhenDeactivated() throws Exception {
      when(templateService.deactivate(any())).thenReturn(null);

      mockMvc
          .perform(put("/templates/{id}/deactivate", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(templateService).deactivate(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(put("/templates/{id}/deactivate", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /templates/{id}")
  class FindById {

    @Test
    @DisplayName("should return 200 with template")
    void shouldReturn200WithTemplate() throws Exception {
      when(templateService.findById(any())).thenReturn(null);

      mockMvc
          .perform(get("/templates/{id}", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(templateService).findById(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/templates/{id}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /templates")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      when(templateService.findAll(any())).thenReturn(Page.empty());

      mockMvc.perform(get("/templates").with(authenticatedJwt())).andExpect(status().isOk());

      verify(templateService).findAll(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get("/templates").with(anonymous())).andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /templates/{id}")
  class Update {

    @Test
    @DisplayName("should return 200 when template is updated")
    void shouldReturn200WhenUpdated() throws Exception {
      when(templateService.update(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              put("/templates/{id}", UUID.randomUUID())
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(TemplateRequestFixtures.updateRequest())))
          .andExpect(status().isOk());

      verify(templateService).update(any(), any());
    }

    @Test
    @DisplayName("should return 400 when required fields are missing")
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
      mockMvc
          .perform(
              put("/templates/{id}", UUID.randomUUID())
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
              put("/templates/{id}", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(TemplateRequestFixtures.updateRequest())))
          .andExpect(status().isUnauthorized());
    }
  }
}
