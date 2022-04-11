package com.csctracker.notifysyncserver.controller;

import com.csctracker.notifysyncserver.dto.Message;
import com.csctracker.notifysyncserver.dto.OutputMessage;
import com.csctracker.notifysyncserver.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExternalMsgController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final NotificationService notificationService;

    public ExternalMsgController(SimpMessagingTemplate simpMessagingTemplate, NotificationService notificationService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.notificationService = notificationService;
    }

    @PostMapping("/info")
    public void envia(@RequestBody Message message) throws JsonProcessingException {
        OutputMessage payload = notificationService.convertMessage(message);
        simpMessagingTemplate.convertAndSend("/topic/messages", payload);
    }
}

