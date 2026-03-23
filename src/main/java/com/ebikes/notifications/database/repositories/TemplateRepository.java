package com.ebikes.notifications.database.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.enums.ChannelType;

public interface TemplateRepository
    extends JpaRepository<Template, UUID>, JpaSpecificationExecutor<Template> {

  boolean existsByName(String name);

  Optional<Template> findByChannelAndNameAndIsActive(
      ChannelType channel, String name, Boolean isActive);
}
