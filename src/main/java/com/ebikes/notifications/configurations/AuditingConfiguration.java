package com.ebikes.notifications.configurations;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.ebikes.notifications.constants.ApplicationConstants;
import com.ebikes.notifications.support.context.ExecutionContext;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class AuditingConfiguration {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(AuditingConfiguration.class);

  @Bean
  public AuditorAware<String> auditorAware() {
    return AuditingConfiguration::tryGetUserId;
  }

  @Bean
  public DateTimeProvider offsetDateTimeProvider() {
    return () -> Optional.of(OffsetDateTime.now());
  }

  private static Optional<String> tryGetUserId() {
    try {
      return Optional.of(ExecutionContext.getUserId());
    } catch (IllegalStateException e) {
      log.warn("AuditorAware: no ExecutionContext present, falling back to SYSTEM_ID");
      return Optional.of(ApplicationConstants.SYSTEM_ID);
    }
  }
}
