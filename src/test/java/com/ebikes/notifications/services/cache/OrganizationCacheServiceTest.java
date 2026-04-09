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

import com.ebikes.notifications.adapters.organizations.OrganizationServiceAdapter;
import com.ebikes.notifications.configurations.properties.CacheProperties;
import com.ebikes.notifications.dtos.adapters.organizations.Branch;
import com.ebikes.notifications.dtos.adapters.organizations.Organization;

@DisplayName("OrganizationCacheService")
@ExtendWith(MockitoExtension.class)
class OrganizationCacheServiceTest {

  private static final String ORGANIZATION_ID_1 = "org-1";
  private static final String ORGANIZATION_ID_2 = "org-2";
  private static final String BRANCH_ID_1 = "branch-1";
  private static final String BRANCH_ID_2 = "branch-2";

  private static final Organization ORGANIZATION_1 =
      new Organization(ORGANIZATION_ID_1, "1 Main St", "Org One", "https://cdn.test/org1.png");
  private static final Organization ORGANIZATION_2 =
      new Organization(ORGANIZATION_ID_2, "2 Side Ave", "Org Two", "https://cdn.test/org2.png");
  private static final Branch BRANCH_1 =
      new Branch(BRANCH_ID_1, "Branch One", "https://cdn.test/b1.png");
  private static final Branch BRANCH_2 =
      new Branch(BRANCH_ID_2, "Branch Two", "https://cdn.test/b2.png");

  private final CacheProperties cacheProperties = new CacheProperties();

  @Mock private OrganizationServiceAdapter adapter;
  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private ValueOperations<String, Object> valueOps;

  private OrganizationCacheService service;

  @BeforeEach
  void setUp() {
    service = new OrganizationCacheService(cacheProperties, adapter, redisTemplate);
  }

  @Nested
  @DisplayName("findOrganizations")
  class FindOrganizations {

    @Test
    @DisplayName("should return all from cache without calling adapter when all ids hit")
    void shouldReturnAllFromCacheOnFullHit() {
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get("notifications:organization:" + ORGANIZATION_ID_1))
          .thenReturn(ORGANIZATION_1);
      when(valueOps.get("notifications:organization:" + ORGANIZATION_ID_2))
          .thenReturn(ORGANIZATION_2);

      Map<String, Organization> result =
          service.findOrganizations(Set.of(ORGANIZATION_ID_1, ORGANIZATION_ID_2));

      assertThat(result)
          .containsEntry(ORGANIZATION_ID_1, ORGANIZATION_1)
          .containsEntry(ORGANIZATION_ID_2, ORGANIZATION_2);
      verify(adapter, never()).findOrganizationsByIds(any());
      verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("should fetch from adapter and cache all when all ids miss")
    void shouldFetchAndCacheAllOnFullMiss() {
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get(any())).thenReturn(null);
      when(adapter.findOrganizationsByIds(Set.of(ORGANIZATION_ID_1, ORGANIZATION_ID_2)))
          .thenReturn(List.of(ORGANIZATION_1, ORGANIZATION_2));

      Map<String, Organization> result =
          service.findOrganizations(Set.of(ORGANIZATION_ID_1, ORGANIZATION_ID_2));

      assertThat(result)
          .containsEntry(ORGANIZATION_ID_1, ORGANIZATION_1)
          .containsEntry(ORGANIZATION_ID_2, ORGANIZATION_2);
      verify(valueOps)
          .set(
              "notifications:organization:" + ORGANIZATION_ID_1,
              ORGANIZATION_1,
              Duration.ofMinutes(30));
      verify(valueOps)
          .set(
              "notifications:organization:" + ORGANIZATION_ID_2,
              ORGANIZATION_2,
              Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("should fetch only misses from adapter and merge with cached hits")
    void shouldFetchOnlyMissesAndMergeWithCache() {
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get("notifications:organization:" + ORGANIZATION_ID_1))
          .thenReturn(ORGANIZATION_1);
      when(valueOps.get("notifications:organization:" + ORGANIZATION_ID_2)).thenReturn(null);
      when(adapter.findOrganizationsByIds(Set.of(ORGANIZATION_ID_2)))
          .thenReturn(List.of(ORGANIZATION_2));

      Map<String, Organization> result =
          service.findOrganizations(Set.of(ORGANIZATION_ID_1, ORGANIZATION_ID_2));

      assertThat(result)
          .containsEntry(ORGANIZATION_ID_1, ORGANIZATION_1)
          .containsEntry(ORGANIZATION_ID_2, ORGANIZATION_2);
      verify(adapter).findOrganizationsByIds(Set.of(ORGANIZATION_ID_2));
      verify(valueOps)
          .set(
              "notifications:organization:" + ORGANIZATION_ID_2,
              ORGANIZATION_2,
              Duration.ofMinutes(30));
      verify(valueOps, never())
          .set(eq("notifications:organization:" + ORGANIZATION_ID_1), any(), any(Duration.class));
    }

    @Test
    @DisplayName(
        "should return empty map without any redis or adapter interaction when input is empty")
    void shouldReturnEmptyMapWhenInputIsEmpty() {
      Map<String, Organization> result = service.findOrganizations(Set.of());

      assertThat(result).isEmpty();
      verify(adapter, never()).findOrganizationsByIds(any());
      verify(redisTemplate, never()).opsForValue();
    }
  }

  @Nested
  @DisplayName("findBranches")
  class FindBranches {

    @Test
    @DisplayName("should return all from cache without calling adapter when all ids hit")
    void shouldReturnAllFromCacheOnFullHit() {
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get("notifications:branch:" + ORGANIZATION_ID_1 + ":" + BRANCH_ID_1))
          .thenReturn(BRANCH_1);
      when(valueOps.get("notifications:branch:" + ORGANIZATION_ID_1 + ":" + BRANCH_ID_2))
          .thenReturn(BRANCH_2);

      Map<String, Branch> result =
          service.findBranches(ORGANIZATION_ID_1, Set.of(BRANCH_ID_1, BRANCH_ID_2));

      assertThat(result).containsEntry(BRANCH_ID_1, BRANCH_1).containsEntry(BRANCH_ID_2, BRANCH_2);
      verify(adapter, never()).findBranchesByIds(any(), any());
      verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("should fetch from adapter and cache all when all ids miss")
    void shouldFetchAndCacheAllOnFullMiss() {
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get(any())).thenReturn(null);
      when(adapter.findBranchesByIds(ORGANIZATION_ID_1, Set.of(BRANCH_ID_1, BRANCH_ID_2)))
          .thenReturn(List.of(BRANCH_1, BRANCH_2));

      Map<String, Branch> result =
          service.findBranches(ORGANIZATION_ID_1, Set.of(BRANCH_ID_1, BRANCH_ID_2));

      assertThat(result).containsEntry(BRANCH_ID_1, BRANCH_1).containsEntry(BRANCH_ID_2, BRANCH_2);
      verify(valueOps)
          .set(
              "notifications:branch:" + ORGANIZATION_ID_1 + ":" + BRANCH_ID_1,
              BRANCH_1,
              Duration.ofMinutes(30));
      verify(valueOps)
          .set(
              "notifications:branch:" + ORGANIZATION_ID_1 + ":" + BRANCH_ID_2,
              BRANCH_2,
              Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("should fetch only misses from adapter and merge with cached hits")
    void shouldFetchOnlyMissesAndMergeWithCache() {
      when(redisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get("notifications:branch:" + ORGANIZATION_ID_1 + ":" + BRANCH_ID_1))
          .thenReturn(BRANCH_1);
      when(valueOps.get("notifications:branch:" + ORGANIZATION_ID_1 + ":" + BRANCH_ID_2))
          .thenReturn(null);
      when(adapter.findBranchesByIds(ORGANIZATION_ID_1, Set.of(BRANCH_ID_2)))
          .thenReturn(List.of(BRANCH_2));

      Map<String, Branch> result =
          service.findBranches(ORGANIZATION_ID_1, Set.of(BRANCH_ID_1, BRANCH_ID_2));

      assertThat(result).containsEntry(BRANCH_ID_1, BRANCH_1).containsEntry(BRANCH_ID_2, BRANCH_2);
      verify(adapter).findBranchesByIds(ORGANIZATION_ID_1, Set.of(BRANCH_ID_2));
      verify(valueOps)
          .set(
              "notifications:branch:" + ORGANIZATION_ID_1 + ":" + BRANCH_ID_2,
              BRANCH_2,
              Duration.ofMinutes(30));
      verify(valueOps, never())
          .set(
              eq("notifications:branch:" + ORGANIZATION_ID_1 + ":" + BRANCH_ID_1),
              any(),
              any(Duration.class));
    }

    @Test
    @DisplayName(
        "should return empty map without any redis or adapter interaction when input is empty")
    void shouldReturnEmptyMapWhenInputIsEmpty() {
      Map<String, Branch> result = service.findBranches(ORGANIZATION_ID_1, Set.of());

      assertThat(result).isEmpty();
      verify(adapter, never()).findBranchesByIds(any(), any());
      verify(redisTemplate, never()).opsForValue();
    }
  }
}
