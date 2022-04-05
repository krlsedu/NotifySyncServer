package com.csctracker.notifysyncserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BaseController {

    @GetMapping("/")
    public String chat(Model model){
        model.addAttribute("title", "Docker + Spring Boot");
        return "index";
    }
}
