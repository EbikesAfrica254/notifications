package com.ebikes.notifications.services.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.ebikes.notifications.adapters.organizations.OrganizationServiceAdapter;
import com.ebikes.notifications.configurations.RedisConfiguration;
import com.ebikes.notifications.configurations.properties.CacheProperties;
import com.ebikes.notifications.dtos.adapters.organizations.Branch;
import com.ebikes.notifications.dtos.adapters.organizations.Organization;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrganizationCacheService {

  private static final String BRANCH_KEY_PREFIX = "notifications:branch:";
  private static final String ORGANIZATION_KEY_PREFIX = "notifications:organization:";

  private final Duration cacheTtl;
  private final OrganizationServiceAdapter adapter;
  private final RedisTemplate<String, Object> redisTemplate;

  public OrganizationCacheService(
      CacheProperties cacheProperties,
      OrganizationServiceAdapter adapter,
      @Qualifier(RedisConfiguration.ORGANIZATION_REDIS_TEMPLATE) RedisTemplate<String, Object> redisTemplate) {
    this.cacheTtl = Duration.ofMinutes(cacheProperties.getTtlMinutes());
    this.adapter = adapter;
    this.redisTemplate = redisTemplate;
  }

  public Map<String, Organization> findOrganizations(Set<String> organizationIds) {
    Map<String, Organization> cached = fetchCachedOrganizations(organizationIds);

    Set<String> misses =
        organizationIds.stream().filter(id -> !cached.containsKey(id)).collect(Collectors.toSet());

    if (!misses.isEmpty()) {
      log.debug("Cache miss for {} organization(s)", misses.size());
      List<Organization> fetched = adapter.findOrganizationsByIds(misses);
      fetched.forEach(
          org -> {
            cached.put(org.id(), org);
            cacheOrganization(org);
          });
    }

    return cached;
  }

  public Map<String, Branch> findBranches(String organizationId, Set<String> branchIds) {
    Map<String, Branch> cached = fetchCachedBranches(organizationId, branchIds);

    Set<String> misses =
        branchIds.stream().filter(id -> !cached.containsKey(id)).collect(Collectors.toSet());

    if (!misses.isEmpty()) {
      log.debug("Cache miss for {} branch(es) in organizationId={}", misses.size(), organizationId);
      List<Branch> fetched = adapter.findBranchesByIds(organizationId, misses);
      fetched.forEach(
          branch -> {
            cached.put(branch.id(), branch);
            cacheBranch(organizationId, branch);
          });
    }

    return cached;
  }

  private Map<String, Organization> fetchCachedOrganizations(Set<String> organizationIds) {
    Map<String, Organization> result = new HashMap<>();
    organizationIds.forEach(
        id -> {
          Object cached = redisTemplate.opsForValue().get(ORGANIZATION_KEY_PREFIX + id);
          if (cached instanceof Organization org) {
            result.put(id, org);
          }
        });
    return result;
  }

  private Map<String, Branch> fetchCachedBranches(String organizationId, Set<String> branchIds) {
    Map<String, Branch> result = new HashMap<>();
    branchIds.forEach(
        id -> {
          Object cached =
              redisTemplate.opsForValue().get(BRANCH_KEY_PREFIX + organizationId + ":" + id);
          if (cached instanceof Branch branch) {
            result.put(id, branch);
          }
        });
    return result;
  }

  private void cacheOrganization(Organization organization) {
    redisTemplate
        .opsForValue()
        .set(ORGANIZATION_KEY_PREFIX + organization.id(), organization, cacheTtl);
  }

  private void cacheBranch(String organizationId, Branch branch) {
    redisTemplate
        .opsForValue()
        .set(BRANCH_KEY_PREFIX + organizationId + ":" + branch.id(), branch, cacheTtl);
  }
}
