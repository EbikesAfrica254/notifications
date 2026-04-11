package com.ebikes.notifications.services.templates.variables.resolvers;

import java.io.Serializable;
import java.util.Map;

import com.ebikes.notifications.dtos.events.incoming.NotificationRequest;

public interface VariableResolver {

  Map<String, Serializable> resolve(NotificationRequest request);
}
