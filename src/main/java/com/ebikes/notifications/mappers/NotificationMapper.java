package com.ebikes.notifications.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.dtos.responses.notifications.NotificationResponse;
import com.ebikes.notifications.dtos.responses.notifications.NotificationSummaryResponse;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(
      target = "recipient",
      expression =
          "java(com.ebikes.notifications.support.security.RecipientMaskingUtility.mask(notification.getRecipient()))")
  @Mapping(target = "templateId", source = "template.id")
  NotificationResponse toResponse(Notification notification);

  @Mapping(
      target = "recipient",
      expression =
          "java(com.ebikes.notifications.support.security.RecipientMaskingUtility.mask(notification.getRecipient()))")
  @Mapping(target = "templateId", source = "template.id")
  NotificationSummaryResponse toSummaryResponse(Notification notification);
}
