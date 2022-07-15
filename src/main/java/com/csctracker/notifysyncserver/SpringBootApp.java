package com.csctracker.notifysyncserver;

import com.csctracker.service.ConfigsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
        "com.csctracker.securitycore",
        "com.csctracker.notifysyncserver",
        "com.csctracker.notifysyncserver.controller",
        "com.csctracker",
        "com.csctracker.service"})
@Import({
        ConfigsService.class
})
public class SpringBootApp extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}