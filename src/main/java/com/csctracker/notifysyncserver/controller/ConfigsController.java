package com.csctracker.notifysyncserver.controller;

import com.csctracker.notifysyncserver.dto.ConfigsDTO;
import com.csctracker.notifysyncserver.model.Configs;
import com.csctracker.notifysyncserver.service.ConfigsService;
import com.csctracker.securitycore.dto.Conversor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
public class ConfigsController {

    private final ConfigsService configsService;

    private final Conversor<Configs, ConfigsDTO> conversor;

    public ConfigsController(ConfigsService configsService) {
        this.configsService = configsService;
        this.conversor = new Conversor<>(Configs.class, ConfigsDTO.class);
    }

    @GetMapping("/configs")
    private ResponseEntity<ConfigsDTO> getConfigs(Principal principal) {
        return new ResponseEntity<>(conversor.toD(configsService.getConfigByUser(principal)), HttpStatus.OK);
    }

    @PostMapping("/configs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    private void setConfigs(@RequestBody ConfigsDTO configsDTO, Principal principal) {
        configsService.save(conversor.toE(configsDTO), principal);
    }
}