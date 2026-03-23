package com.ebikes.notifications.adapters.sse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ebikes.notifications.configurations.properties.NotificationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class SseConnectionManager {

  private final NotificationProperties notificationProperties;

  private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();

  private String buildConnectionKey(String userId) {
    return "sse" + ":" + userId;
  }

  public SseEmitter createConnection(String userId) {
    if (!notificationProperties.getChannels().getSse().isEnabled()) {
      throw new IllegalStateException("SSE channel is currently disabled");
    }

    String connectionKey = buildConnectionKey(userId);

    SseEmitter existingConnection = connections.get(connectionKey);
    if (existingConnection != null) {
      log.info("Replacing existing SSE connection - userId={}", userId);
      try {
        existingConnection.complete();
      } catch (Exception e) {
        log.debug("Error completing existing connection - will proceed with new connection", e);
      }
    }

    SseEmitter emitter = getSseEmitter(userId);

    connections.put(connectionKey, emitter);

    log.info(
        "SSE connection established - userId={} timeout={}ms totalConnections={}",
        userId,
        notificationProperties.getChannels().getSse().getConnectionTimeout(),
        connections.size());

    return emitter;
  }

  public SseEmitter getConnection(String userId) {
    String connectionKey = buildConnectionKey(userId);
    return connections.get(connectionKey);
  }

  private @NonNull SseEmitter getSseEmitter(String userId) {
    SseEmitter emitter =
        new SseEmitter(notificationProperties.getChannels().getSse().getConnectionTimeout());

    emitter.onTimeout(
        () -> {
          log.info("SSE connection timeout - userId={}", userId);
          removeConnection(userId);
        });

    emitter.onCompletion(
        () -> {
          log.info("SSE connection completed - userId={}", userId);
          removeConnection(userId);
        });

    emitter.onError(
        throwable -> {
          log.warn("SSE connection error - userId={} error={}", userId, throwable.getMessage());
          removeConnection(userId);
        });
    return emitter;
  }

  public void removeConnection(String userId) {
    String connectionKey = buildConnectionKey(userId);
    SseEmitter removed = connections.remove(connectionKey);

    if (removed != null) {
      log.debug(
          "SSE connection removed - userId={} remainingConnections={}", userId, connections.size());
    }
  }
}
