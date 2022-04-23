package com.csctracker.notifysyncserver.repository;

import com.csctracker.notifysyncserver.model.Message;
import com.csctracker.securitycore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationSyncRepository extends JpaRepository<Message, Long> {
    List<Message> findByUserAndDateSentIsNull(User user);

    Message findByUserAndId(User user, Long id);
}

