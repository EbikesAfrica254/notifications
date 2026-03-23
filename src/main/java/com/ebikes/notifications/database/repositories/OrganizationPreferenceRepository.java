package com.ebikes.notifications.database.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ebikes.notifications.database.entities.OrganizationChannelPreference;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;

public interface OrganizationPreferenceRepository
    extends JpaRepository<OrganizationChannelPreference, UUID>,
        JpaSpecificationExecutor<OrganizationChannelPreference> {

  List<OrganizationChannelPreference> findByOrganizationId(String organizationId);

  Optional<OrganizationChannelPreference> findByOrganizationIdAndChannelAndCategory(
      String organizationId, ChannelType channel, NotificationCategory category);
}
