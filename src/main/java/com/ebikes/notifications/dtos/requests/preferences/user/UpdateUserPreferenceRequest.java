package com.ebikes.notifications.dtos.requests.preferences.user;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

public record UpdateUserPreferenceRequest(@NotNull(message = "Enabled is required") Boolean enabled)
    implements Serializable {}
