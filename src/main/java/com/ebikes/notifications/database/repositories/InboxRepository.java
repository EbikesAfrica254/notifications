package com.ebikes.notifications.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ebikes.notifications.database.entities.Inbox;

@Repository
public interface InboxRepository extends JpaRepository<Inbox, String> {}
