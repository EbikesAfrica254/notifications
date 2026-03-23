package com.ebikes.notifications.dtos.requests.preferences.organization;

import java.io.Serializable;

public record UpdateOrganizationPreferenceRequest(Boolean enabled) implements Serializable {}
