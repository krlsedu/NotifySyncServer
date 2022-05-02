package com.csctracker.notifysyncserver.controller;

import com.csctracker.securitycore.dto.TokenDTO;
import com.csctracker.securitycore.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BaseController {

    private final AuthService authService;

    public BaseController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String chat(Model model,
                       @RequestParam(value = "redirect_uri", required = false) String url,
                       @RequestParam(value = "code", required = false) String autorazationCode) throws UnirestException, JsonProcessingException {
        model.addAttribute("title", "");
        if (autorazationCode != null) {
            var token = authService.getToken(autorazationCode, url);
            model.addAttribute("tokenDTO", token);
        } else {
            model.addAttribute("tokenDTO", new TokenDTO());
        }
        return "index";
    }
}
