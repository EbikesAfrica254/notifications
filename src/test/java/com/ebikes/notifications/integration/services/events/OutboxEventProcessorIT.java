package com.ebikes.notifications.integration.services.events;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ebikes.notifications.constants.ApplicationConstants;
import com.ebikes.notifications.constants.EventConstants.DomainEvents;
import com.ebikes.notifications.database.entities.Outbox;
import com.ebikes.notifications.database.repositories.OutboxRepository;
import com.ebikes.notifications.enums.OutboxStatus;
import com.ebikes.notifications.services.events.OutboxEventProcessor;
import com.ebikes.notifications.support.infrastructure.AbstractIntegrationTest;

class OutboxEventProcessorIT extends AbstractIntegrationTest {

  @Autowired private OutboxEventProcessor outboxEventProcessor;
  @Autowired private OutboxRepository outboxRepository;

  @BeforeEach
  void setUp() {
    outboxRepository.deleteAll();
  }

  private Outbox seedPendingOutbox() {
    return outboxRepository.save(
        Outbox.builder()
            .eventType(DomainEvents.Notifications.CREATED)
            .payload(Map.of("documentId", "doc-001"))
            .routingKey("notifications.notification.audit")
            .build());
  }

  private Outbox seedOutboxNearDeadLetter() {
    Outbox outbox =
        Outbox.builder()
            .eventType(DomainEvents.Notifications.CREATED)
            .payload(Map.of("documentId", "doc-001"))
            .routingKey("notifications.notification.audit")
            .retryCount(ApplicationConstants.Outbox.MAX_RETRY_COUNT - 1)
            .status(OutboxStatus.PENDING)
            .build();
    return outboxRepository.save(outbox);
  }

  @Nested
  class Process {

    @Test
    void marksOutboxAsSentOnSuccessfulPublish() {
      Outbox outbox = seedPendingOutbox();

      outboxEventProcessor.process(outbox);

      Outbox saved = outboxRepository.findById(outbox.getId()).orElseThrow();
      assertThat(saved.getStatus()).isEqualTo(OutboxStatus.SENT);
      assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Nested
    class ProcessWithBrokerFailure {

      @MockitoBean StreamBridge streamBridge;

      @Test
      void marksOutboxAsFailedWhenStreamBridgeReturnsFalse() {
        when(streamBridge.send(any(), any())).thenReturn(false);
        Outbox outbox = seedPendingOutbox();

        outboxEventProcessor.process(outbox);

        Outbox saved = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(saved.getRetryCount()).isEqualTo(1);
      }

      @Test
      void marksOutboxAsDeadLetterWhenMaxRetriesExceeded() {
        when(streamBridge.send(any(), any())).thenReturn(false);
        Outbox outbox = seedOutboxNearDeadLetter();

        outboxEventProcessor.process(outbox);

        Outbox saved = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(saved.getRetryCount()).isEqualTo(ApplicationConstants.Outbox.MAX_RETRY_COUNT);
      }
    }
  }
}
