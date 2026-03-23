package com.ebikes.notifications.database.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ebikes.notifications.database.entities.Delivery;

public interface DeliveryRepository
    extends JpaRepository<Delivery, UUID>, JpaSpecificationExecutor<Delivery> {}
