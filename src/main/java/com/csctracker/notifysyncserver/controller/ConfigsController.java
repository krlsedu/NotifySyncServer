package com.csctracker.notifysyncserver.controller;

import com.csctracker.dto.Conversor;
import com.csctracker.model.Configs;
import com.csctracker.notifysyncserver.dto.ConfigsDTO;
import com.csctracker.service.ConfigsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ConfigsController {

    private final ConfigsService configsService;

    private final Conversor<Configs, ConfigsDTO> conversor;

    @Autowired
    public ConfigsController(ConfigsService configsService) {
        this.configsService = configsService;
        this.conversor = new Conversor<>(Configs.class, ConfigsDTO.class);
    }

    @GetMapping("/configs")
    private ResponseEntity<ConfigsDTO> getConfigs() {
        return new ResponseEntity<>(conversor.toD(configsService.getConfigByUser()), HttpStatus.OK);
    }

    @PostMapping(value = "/configs", consumes = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    private void setConfigs(@RequestBody ConfigsDTO configsDTO) {
        configsService.save(conversor.toE(configsDTO));
    }
}
