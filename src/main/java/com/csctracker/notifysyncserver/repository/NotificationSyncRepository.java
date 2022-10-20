package com.csctracker.notifysyncserver.repository;

import com.csctracker.model.User;
import com.csctracker.notifysyncserver.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface NotificationSyncRepository extends JpaRepository<Message, Long> {
    List<Message> findByUserAndDateSentIsNull(User user);

    List<Message> findByDateSentIsNullAndDateSyncedBetween(Date date, Date date1);

    Message findByUserAndUuid(User user, String uuid);

    List<Message> findByUserAndAppIsNotNullOrderByIdDesc(User user, Pageable pageable);

    List<Message> findByUserAndAppIsNotNullAndDateSyncedGreaterThanOrderByIdAsc(User user, Date date);
}

