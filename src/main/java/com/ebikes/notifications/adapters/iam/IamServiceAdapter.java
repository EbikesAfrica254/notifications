package com.ebikes.notifications.adapters.iam;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ebikes.notifications.dtos.adapters.iam.UserDetails;
import com.ebikes.notifications.dtos.responses.api.SuccessResponse;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IamServiceAdapter {

  private static final String USERS_REFERENCE_ENDPOINT = "/users/reference";

  private final RestClient restClient;

  public IamServiceAdapter(@Qualifier("iamServiceRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  public List<UserDetails> findUsersByIds(Set<String> userIds) {
    log.debug("Fetching user references: count={}", userIds.size());

    try {
      SuccessResponse<List<UserDetails>> response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder.path(USERS_REFERENCE_ENDPOINT).queryParam("ids", userIds).build())
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.data() == null) {
        throw new ExternalServiceException(
            USERS_REFERENCE_ENDPOINT,
            "IAM service returned empty response",
            ResponseCode.EXTERNAL_SERVICE_ERROR);
      }

      return response.data();

    } catch (RestClientException e) {
      log.error("Failed to fetch user references: userIds={}", userIds, e);
      throw new ExternalServiceException(
          USERS_REFERENCE_ENDPOINT,
          "Failed to fetch user references: " + e.getMessage(),
          ResponseCode.fromHttpStatus(extractStatus(e)),
          e);
    }
  }

  private int extractStatus(RestClientException e) {
    if (e instanceof HttpStatusCodeException statusEx) {
      return statusEx.getStatusCode().value();
    }
    return 500;
  }
}
