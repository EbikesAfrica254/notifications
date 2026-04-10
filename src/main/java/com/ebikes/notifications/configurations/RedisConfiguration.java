package com.ebikes.notifications.configurations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.ebikes.notifications.configurations.properties.CacheProperties;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Configuration
@Slf4j
public class RedisConfiguration {

  public static final String IAM_REDIS_TEMPLATE = "iamRedisTemplate";
  public static final String ORGANIZATION_REDIS_TEMPLATE = "organizationRedisTemplate";

  @Bean
  public RedisCacheConfiguration cacheConfiguration(CacheProperties cacheProperties) {
    return RedisCacheConfiguration.defaultCacheConfig()
        .disableCachingNullValues()
        .computePrefixWith(
            cacheName -> cacheProperties.getOrganizations().getKeyPrefix() + ":" + cacheName + ":")
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                GenericJacksonJsonRedisSerializer.builder().build()));
  }

  @Bean
  @ConditionalOnProperty(
      value = {"spring.data.redis.ssl.enabled"},
      havingValue = "true")
  public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
    return clientConfigurationBuilder -> {
      log.info("lettuceClientConfigurationBuilderCustomizer : disabling peer verification");
      clientConfigurationBuilder.useSsl().disablePeerVerification();
    };
  }

  @Bean(IAM_REDIS_TEMPLATE)
  public RedisTemplate<String, Object> iamRedisTemplate(
      RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
    return buildRedisTemplate(connectionFactory, objectMapper);
  }

  @Bean(ORGANIZATION_REDIS_TEMPLATE)
  public RedisTemplate<String, Object> organizationRedisTemplate(
      RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
    return buildRedisTemplate(connectionFactory, objectMapper);
  }

  private RedisTemplate<String, Object> buildRedisTemplate(
      RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
    GenericJacksonJsonRedisSerializer serializer =
        new GenericJacksonJsonRedisSerializer(objectMapper);
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(serializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(serializer);
    template.afterPropertiesSet();
    return template;
  }
}
