package com.ebikes.notifications.controllers;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ebikes.notifications.adapters.channels.sse.SseConnectionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@RestController
@RequestMapping("/sse")
@Slf4j
public class SseController {

  private final SseConnectionManager connectionManager;

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamNotifications(Authentication authentication) {
    JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
    String userId = jwtAuth.getToken().getSubject();

    log.info("SSE connection request - userId={}", userId);

    return connectionManager.createConnection(userId);
  }
}
