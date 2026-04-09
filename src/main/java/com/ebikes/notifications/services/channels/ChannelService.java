package com.ebikes.notifications.services.channels;

import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.responses.channels.ChannelResponse;
import com.ebikes.notifications.enums.ChannelType;

public interface ChannelService {

  ChannelType getChannelType();

  ChannelResponse send(Notification notification);
}
