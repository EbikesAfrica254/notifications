package com.ebikes.notifications.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.dtos.responses.deliveries.DeliveryResponse;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface DeliveryMapper {

  @Mapping(source = "createdAt", target = "attemptedAt")
  @Mapping(source = "notification.id", target = "notificationId")
  DeliveryResponse toResponse(Delivery delivery);
}
