package com.ebikes.notifications.adapters.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ebikes.notifications.dtos.adapters.iam.UserDetails;
import com.ebikes.notifications.dtos.responses.api.SuccessResponse;
import com.ebikes.notifications.exceptions.ExternalServiceException;

@DisplayName("IamServiceAdapter")
@ExtendWith(MockitoExtension.class)
class IamServiceAdapterTest {

  private static final String USER_ID = "user-1";

  @Mock private RestClient restClient;

  private IamServiceAdapter adapter;
  private RestClient.ResponseSpec responseSpec;

  @BeforeEach
  @SuppressWarnings({"unchecked", "rawtypes"})
  void setUp() {
    adapter = new IamServiceAdapter(restClient);

    RestClient.RequestHeadersUriSpec requestHeadersUriSpec =
        mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec<?> requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    responseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
  }

  @Nested
  @DisplayName("findUsersByIds")
  class FindUsersByIds {

    @Test
    @DisplayName("should return users from successful response")
    @SuppressWarnings("unchecked")
    void shouldReturnUsers() {
      List<UserDetails> users = List.of(new UserDetails(USER_ID, "Jane", "Doe", "jane.doe"));
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenReturn(SuccessResponse.of(users));

      List<UserDetails> result = adapter.findUsersByIds(Set.of(USER_ID));

      assertThat(result).containsExactlyElementsOf(users);
    }

    @Test
    @DisplayName("should throw ExternalServiceException when response is null")
    @SuppressWarnings("unchecked")
    void shouldThrowWhenResponseIsNull() {
      when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(null);

      Set<String> userIds = Set.of(USER_ID);
      assertThatThrownBy(() -> adapter.findUsersByIds(userIds))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should throw ExternalServiceException when response data is null")
    @SuppressWarnings("unchecked")
    void shouldThrowWhenResponseDataIsNull() {
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenReturn(new SuccessResponse<>("SUCCESS", null, null));

      Set<String> userIds = Set.of(USER_ID);
      assertThatThrownBy(() -> adapter.findUsersByIds(userIds))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should wrap RestClientException as ExternalServiceException")
    @SuppressWarnings("unchecked")
    void shouldWrapRestClientException() {
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenThrow(new RestClientException("connection refused"));

      Set<String> userIds = Set.of(USER_ID);
      assertThatThrownBy(() -> adapter.findUsersByIds(userIds))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should map HTTP status from HttpStatusCodeException")
    @SuppressWarnings("unchecked")
    void shouldMapHttpStatusFromStatusCodeException() {
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenThrow(
              HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

      Set<String> userIds = Set.of(USER_ID);
      assertThatThrownBy(() -> adapter.findUsersByIds(userIds))
          .isInstanceOf(ExternalServiceException.class);
    }
  }
}
