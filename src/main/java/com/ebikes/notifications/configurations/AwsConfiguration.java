package com.ebikes.notifications.configurations;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ebikes.notifications.configurations.properties.NotificationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AwsConfiguration {

  private final NotificationProperties notificationProperties;

  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean(SesClient.class)
  public SesClient sesClient() {
    NotificationProperties.Ses ses = notificationProperties.getChannels().getEmail().getSes();

    SesClientBuilder builder =
        SesClient.builder()
            .region(Region.of(ses.getRegion()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build());

    if (ses.getEndpoint() != null && !ses.getEndpoint().isEmpty()) {
      URI endpointUri = URI.create(ses.getEndpoint());
      builder.endpointOverride(endpointUri);
      log.info("SesClient configured with endpoint override: {} (LocalStack/custom)", endpointUri);
    } else {
      log.info("SesClient configured for AWS region: {}", ses.getRegion());
    }

    return builder.build();
  }
}
