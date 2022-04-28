package com.csctracker.notifysyncserver.repository;

import com.csctracker.notifysyncserver.model.Configs;
import com.csctracker.securitycore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigsRepository extends JpaRepository<Configs, Long> {
    Configs findByUser(User user);

    boolean existsByUser(User user);
}
