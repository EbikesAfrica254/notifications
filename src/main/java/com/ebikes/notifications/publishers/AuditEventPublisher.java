package com.ebikes.notifications.publishers;

import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.ebikes.notifications.constants.MDCKeys;
import com.ebikes.notifications.dtos.events.outgoing.AuditEvent;
import com.ebikes.notifications.enums.AuditOutcome;
import com.ebikes.notifications.services.events.OutboxService;
import com.ebikes.notifications.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

  private final OutboxService outboxService;

  public void publishFailure(
      UUID entityId,
      String entityType,
      String eventType,
      String failureReason,
      Map<String, String> metadata,
      String organizationId,
      String routingKey) {

    var event =
        new AuditEvent(
            entityId,
            entityType,
            eventType,
            failureReason,
            MDC.get(MDCKeys.IP_ADDRESS),
            metadata,
            organizationId,
            AuditOutcome.FAILURE,
            null,
            null,
            ExecutionContext.getUserId());

    outboxService.save(eventType, event, routingKey);
  }

  public void publishSuccess(
      UUID entityId,
      String entityType,
      String eventType,
      Map<String, String> metadata,
      String organizationId,
      String routingKey,
      String userId) {

    var event =
        new AuditEvent(
            entityId,
            entityType,
            eventType,
            null,
            MDC.get(MDCKeys.IP_ADDRESS),
            metadata,
            organizationId,
            AuditOutcome.SUCCESS,
            null,
            null,
            userId);

    outboxService.save(eventType, event, routingKey);
  }
}
