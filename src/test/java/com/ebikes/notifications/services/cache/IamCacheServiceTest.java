package com.ebikes.notifications.services.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.ebikes.notifications.adapters.iam.IamServiceAdapter;
import com.ebikes.notifications.configurations.properties.CacheProperties;
import com.ebikes.notifications.dtos.adapters.iam.UserDetails;

@DisplayName("IamCacheService")
@ExtendWith(MockitoExtension.class)
class IamCacheServiceTest {

  private static final String USER_ID_1 = "user-1";
  private static final String USER_ID_2 = "user-2";

  private static final UserDetails USER_1 = new UserDetails(USER_ID_1, "Jane", "Doe", "jane.doe");
  private static final UserDetails USER_2 =
      new UserDetails(USER_ID_2, "John", "Smith", "john.smith");

  private final CacheProperties cacheProperties = new CacheProperties();

  @Mock private IamServiceAdapter adapter;
  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private ValueOperations<String, Object> valueOps;

  private IamCacheService service;

  @BeforeEach
  void setUp() {
    service = new IamCacheService(cacheProperties, adapter, redisTemplate);
  }

  @Nested
  @DisplayName("findUsers")
  class FindUsers {

    @Test
    @DisplayName("should return all from cache without calling adapter when all ids hit")
    void shouldReturnAllFromCacheOnFullHit() {
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get("notifications:user:" + USER_ID_1)).thenReturn(USER_1);
      when(valueOps.get("notifications:user:" + USER_ID_2)).thenReturn(USER_2);

      Map<String, UserDetails> result = service.findUsers(Set.of(USER_ID_1, USER_ID_2));

      assertThat(result).containsEntry(USER_ID_1, USER_1).containsEntry(USER_ID_2, USER_2);
      verify(adapter, never()).findUsersByIds(any());
      verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("should fetch from adapter and cache all when all ids miss")
    void shouldFetchAndCacheAllOnFullMiss() {
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get(any())).thenReturn(null);
      when(adapter.findUsersByIds(Set.of(USER_ID_1, USER_ID_2)))
          .thenReturn(List.of(USER_1, USER_2));

      Map<String, UserDetails> result = service.findUsers(Set.of(USER_ID_1, USER_ID_2));

      assertThat(result).containsEntry(USER_ID_1, USER_1).containsEntry(USER_ID_2, USER_2);
      verify(valueOps).set("notifications:user:" + USER_ID_1, USER_1, Duration.ofMinutes(30));
      verify(valueOps).set("notifications:user:" + USER_ID_2, USER_2, Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("should fetch only misses from adapter and merge with cached hits")
    void shouldFetchOnlyMissesAndMergeWithCache() {
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get("notifications:user:" + USER_ID_1)).thenReturn(USER_1);
      when(valueOps.get("notifications:user:" + USER_ID_2)).thenReturn(null);
      when(adapter.findUsersByIds(Set.of(USER_ID_2))).thenReturn(List.of(USER_2));

      Map<String, UserDetails> result = service.findUsers(Set.of(USER_ID_1, USER_ID_2));

      assertThat(result).containsEntry(USER_ID_1, USER_1).containsEntry(USER_ID_2, USER_2);
      verify(adapter).findUsersByIds(Set.of(USER_ID_2));
      verify(valueOps).set("notifications:user:" + USER_ID_2, USER_2, Duration.ofMinutes(30));
      verify(valueOps, never())
          .set(eq("notifications:user:" + USER_ID_1), any(), any(Duration.class));
    }
  }
}
