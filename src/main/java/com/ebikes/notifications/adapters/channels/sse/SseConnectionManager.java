package com.ebikes.notifications.adapters.channels.sse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ebikes.notifications.configurations.properties.NotificationProperties;
import com.ebikes.notifications.enums.ResponseCode;
import com.ebikes.notifications.exceptions.InvalidStateException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class SseConnectionManager {

  private final NotificationProperties notificationProperties;

  private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();
  private final Map<String, ReentrantLock> connectionLocks = new ConcurrentHashMap<>();

  private String buildConnectionKey(String recipient) {
    return "sse:" + recipient;
  }

  public SseEmitter createConnection(String recipient) {
    if (!notificationProperties.getChannels().getSse().isEnabled()) {
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE, "SSE channel is currently disabled");
    }

    enforceConnectionLimit();

    String key = buildConnectionKey(recipient);

    SseEmitter emitter = buildEmitter(recipient);
    ReentrantLock lock = new ReentrantLock();

    connections.compute(
        key,
        (k, existing) -> {
          if (existing != null) {
            log.info("Replacing existing SSE connection - recipient={}", recipient);
          }
          connectionLocks.put(k, lock);
          return emitter;
        });

    log.info(
        "SSE connection established - recipient={} timeout={}ms totalConnections={}",
        recipient,
        notificationProperties.getChannels().getSse().getConnectionTimeout(),
        connections.size());

    return emitter;
  }

  public boolean hasConnection(String recipient) {
    return connections.containsKey(buildConnectionKey(recipient));
  }

  public void removeConnection(String recipient) {
    String key = buildConnectionKey(recipient);

    connections.compute(
        key,
        (k, existing) -> {
          if (existing != null) {
            connectionLocks.remove(k);
            log.debug(
                "SSE connection removed - recipient={} remainingConnections={}",
                recipient,
                connections.size() - 1);
          }
          return null;
        });
  }

  public boolean sendToUser(String recipient, SseEmitter.SseEventBuilder event) {
    String key = buildConnectionKey(recipient);

    ReentrantLock lock = connectionLocks.get(key);
    SseEmitter emitter = connections.get(key);

    if (lock == null || emitter == null) {
      return false;
    }

    lock.lock();
    try {
      emitter.send(event);
      return true;
    } catch (IOException e) {
      log.warn("SSE send failed - recipient={} error={}", recipient, e.getMessage());
      removeConnection(recipient);
      return false;
    } finally {
      lock.unlock();
    }
  }

  @Scheduled(fixedDelayString = "${notification.channels.sse.heartbeat-interval}")
  void sendHeartbeat() {
    if (connections.isEmpty()) {
      return;
    }

    log.debug("Sending SSE heartbeat - activeConnections={}", connections.size());

    connections.forEach(
        (key, emitter) -> {
          String recipient = key.substring("sse:".length());
          ReentrantLock lock = connectionLocks.get(key);

          if (lock == null) {
            removeConnection(recipient);
            return;
          }

          lock.lock();
          try {
            emitter.send(SseEmitter.event().comment("ping"));
          } catch (IOException e) {
            log.debug("SSE heartbeat failed - recipient={}, removing connection", recipient);
            removeConnection(recipient);
          } finally {
            lock.unlock();
          }
        });
  }

  private SseEmitter buildEmitter(String recipient) {
    SseEmitter emitter =
        new SseEmitter(notificationProperties.getChannels().getSse().getConnectionTimeout());

    emitter.onCompletion(
        () -> {
          log.debug("SSE connection completed - recipient={}", recipient);
          removeConnection(recipient);
        });

    emitter.onError(
        throwable -> {
          log.warn(
              "SSE connection error - recipient={} error={}", recipient, throwable.getMessage());
          removeConnection(recipient);
        });

    emitter.onTimeout(
        () -> {
          log.info("SSE connection timed out - recipient={}", recipient);
          removeConnection(recipient);
        });

    return emitter;
  }

  private void enforceConnectionLimit() {
    int limit = notificationProperties.getChannels().getSse().getConnectionLimit();
    if (connections.size() >= limit) {
      log.warn("SSE connection limit reached - limit={}", limit);
      throw new InvalidStateException(
          ResponseCode.INVALID_STATE, "SSE connection limit of " + limit + " reached");
    }
  }
}
