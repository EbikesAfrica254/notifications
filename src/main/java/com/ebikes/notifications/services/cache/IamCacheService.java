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

import com.ebikes.notifications.adapters.iam.IamServiceAdapter;
import com.ebikes.notifications.configurations.RedisConfiguration;
import com.ebikes.notifications.configurations.properties.CacheProperties;
import com.ebikes.notifications.dtos.adapters.iam.UserDetails;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IamCacheService {

  private static final String USER_KEY_PREFIX = "notifications:user:";

  private final Duration cacheTtl;
  private final IamServiceAdapter adapter;
  private final RedisTemplate<String, Object> redisTemplate;

  public IamCacheService(
      CacheProperties cacheProperties,
      IamServiceAdapter adapter,
      @Qualifier(RedisConfiguration.IAM_REDIS_TEMPLATE) RedisTemplate<String, Object> redisTemplate) {
    this.cacheTtl = Duration.ofMinutes(cacheProperties.getIam().getTtlMinutes());
    this.adapter = adapter;
    this.redisTemplate = redisTemplate;
  }

  public Map<String, UserDetails> findUsers(Set<String> userIds) {
    Map<String, UserDetails> cached = fetchCachedUsers(userIds);
    Set<String> misses =
        userIds.stream().filter(id -> !cached.containsKey(id)).collect(Collectors.toSet());

    if (!misses.isEmpty()) {
      log.debug("Cache miss for {} user(s)", misses.size());
      List<UserDetails> fetched = adapter.findUsersByIds(misses);
      fetched.forEach(
          user -> {
            cached.put(user.keycloakUserId(), user);
            cacheUser(user);
          });
    }

    return cached;
  }

  private Map<String, UserDetails> fetchCachedUsers(Set<String> userIds) {
    Map<String, UserDetails> result = new HashMap<>();
    userIds.forEach(
        id -> {
          Object cached = redisTemplate.opsForValue().get(USER_KEY_PREFIX + id);
          if (cached instanceof UserDetails user) {
            result.put(id, user);
          }
        });
    return result;
  }

  private void cacheUser(UserDetails user) {
    redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.keycloakUserId(), user, cacheTtl);
  }
}
