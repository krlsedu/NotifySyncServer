package com.csctracker.notifysyncserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.csctracker", "com.csctracker.securitycore", "com.csctracker.notifysyncserver"})
public class SpringBootApp extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}