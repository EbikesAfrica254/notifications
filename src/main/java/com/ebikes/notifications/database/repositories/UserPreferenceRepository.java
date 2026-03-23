package com.ebikes.notifications.database.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ebikes.notifications.database.entities.UserChannelPreference;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;

public interface UserPreferenceRepository
    extends JpaRepository<UserChannelPreference, UUID>,
        JpaSpecificationExecutor<UserChannelPreference> {

  boolean existsByUserId(String userId);

  List<UserChannelPreference> findByUserIdAndOrganizationId(String userId, String organizationId);

  Optional<UserChannelPreference> findByUserIdAndOrganizationIdAndChannelAndCategory(
      String userId, String organizationId, ChannelType channel, NotificationCategory category);
}
