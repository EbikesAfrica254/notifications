package com.ebikes.notifications.dtos.internal;

import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;

public record PreferenceKey(ChannelType channel, NotificationCategory category) {}
