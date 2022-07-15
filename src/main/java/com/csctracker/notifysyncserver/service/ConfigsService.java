package com.csctracker.notifysyncserver.service;

import com.csctracker.model.Configs;
import com.csctracker.model.User;
import com.csctracker.repository.ConfigsRepository;
import com.csctracker.securitycore.service.UserInfoService;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.TimeZone;

@Service("configsServiceNotifySyncServer")
public class ConfigsService {
    private final ConfigsRepository configsRepository;

    private final UserInfoService userInfoService;

    public ConfigsService(ConfigsRepository configsRepository, UserInfoService userInfoService) {
        this.configsRepository = configsRepository;
        this.userInfoService = userInfoService;
    }

    public Configs getConfigByUser() {
        return configsRepository.findByUser(userInfoService.getUser());
    }

    public void save(Configs configs, Principal principal) {
        User user = userInfoService.getUser(principal);
        if (!configsRepository.existsByUser(user)) {
            configs.setUser(user);
        } else {
            Configs byUser = configsRepository.findByUser(user);
            configs.setId(byUser.getId());
            configs.setUser(user);
            if (configs.getTimeZone() == null) {
                configs.setTimeZone(byUser.getTimeZone());
            }
            if (configs.getFavoriteContact() == null) {
                configs.setFavoriteContact(byUser.getFavoriteContact());
            }
            if (configs.getApplicationNotify() == null) {
                configs.setApplicationNotify(byUser.getApplicationNotify());
            }
            if (configs.getTimeZone() == null) {
                configs.setTimeZone("America/Sao_Paulo");
            }
        }
        configsRepository.save(configs);
    }


    public TimeZone getTimeZone() {
        String timeZone = null;
        try {
            timeZone = getConfigByUser().getTimeZone();
        } catch (Exception e) {
            //
        }

        TimeZone timeZone1 = null;
        try {
            timeZone1 = TimeZone.getTimeZone(timeZone == null ? "America/Sao_Paulo" : timeZone);
        } catch (Exception e) {
            timeZone1 = TimeZone.getTimeZone("America/Sao_Paulo");
        }
        return timeZone1;
    }
}
