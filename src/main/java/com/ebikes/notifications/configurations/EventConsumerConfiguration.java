package com.ebikes.notifications.configurations;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.ebikes.notifications.listeners.IncomingEventListener;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class EventConsumerConfiguration {

  private final IncomingEventListener incomingEventListener;

  @Bean
  public Consumer<Message<?>> incomingEventConsumer() {
    return incomingEventListener::route;
  }
}
