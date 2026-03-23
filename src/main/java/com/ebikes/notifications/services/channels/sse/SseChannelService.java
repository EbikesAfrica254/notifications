package com.ebikes.notifications.services.channels.sse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ebikes.notifications.adapters.sse.SseConnectionManager;
import com.ebikes.notifications.dtos.requests.channels.sse.SseRequest;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.ExternalServiceException;
import com.ebikes.notifications.exceptions.ResourceNotFoundException;
import com.ebikes.notifications.support.references.ReferenceGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class SseChannelService {

  private static final String COST_CURRENCY = "KES";
  private static final String ENDPOINT = "sse://send-event";

  private final SseConnectionManager connectionManager;

  public ChannelResponse send(SseRequest request) {
    String userId = request.userId();

    log.debug("Routing SSE request - userId={}", userId);

    SseEmitter emitter = connectionManager.getConnection(userId);

    if (emitter == null) {
      log.warn("No active SSE connection - userId={}", userId);
      throw new ResourceNotFoundException(
          ResponseCode.RESOURCE_NOT_FOUND, "No active SSE connection for user: " + userId);
    }

    String messageId = ReferenceGenerator.generateMessageReference(ChannelType.SSE);

    try {
      emitter.send(SseEmitter.event().data(request.data()).id(messageId).name("notification"));

      log.info("SSE message sent - userId={}, messageId={}", userId, messageId);

      return new ChannelResponse(
          BigDecimal.ZERO,
          COST_CURRENCY,
          Map.of("userId", userId),
          messageId,
          ENDPOINT,
          OffsetDateTime.now());

    } catch (IOException e) {
      log.error("Failed to send SSE message - userId={}, error={}", userId, e.getMessage(), e);
      connectionManager.removeConnection(userId);
      throw new ExternalServiceException(
          ENDPOINT,
          "Failed to send SSE message: " + e.getMessage(),
          ResponseCode.EXTERNAL_SERVICE_ERROR,
          e);
    }
  }
}
