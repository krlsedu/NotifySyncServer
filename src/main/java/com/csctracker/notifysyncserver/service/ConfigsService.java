package com.csctracker.notifysyncserver.service;

import com.csctracker.notifysyncserver.model.Configs;
import com.csctracker.notifysyncserver.repository.ConfigsRepository;
import com.csctracker.securitycore.model.User;
import com.csctracker.securitycore.service.UserInfoService;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class ConfigsService {
    private final ConfigsRepository configsRepository;

    private final UserInfoService userInfoService;

    public ConfigsService(ConfigsRepository configsRepository, UserInfoService userInfoService) {
        this.configsRepository = configsRepository;
        this.userInfoService = userInfoService;
    }

    public Configs getConfigByUser(Principal principal) {
        return configsRepository.findByUser(userInfoService.getUser(principal));
    }

    public void save(Configs configs, Principal principal) {
        User user = userInfoService.getUser(principal);
        if (!configsRepository.existsByUser(user)) {
            configs.setUser(user);
        } else {
            configs.setId(configsRepository.findByUser(user).getId());
            configs.setUser(user);
        }
        configsRepository.save(configs);
    }
}
