package com.ebikes.notifications.database.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ebikes.notifications.database.entities.Delivery;

public interface DeliveryRepository
    extends JpaRepository<Delivery, UUID>, JpaSpecificationExecutor<Delivery> {

  @EntityGraph(attributePaths = "notification")
  List<Delivery> findByNotificationIdOrderByAttemptNumberAsc(UUID notificationId);
}
