package com.csctracker.notifysyncserver.controller;

import com.csctracker.notifysyncserver.dto.MessageDTO;
import com.csctracker.notifysyncserver.dto.OutputMessage;
import com.csctracker.notifysyncserver.model.Message;
import com.csctracker.notifysyncserver.service.NotificationService;
import com.csctracker.securitycore.dto.Conversor;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
public class ExternalMsgController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final NotificationService notificationService;

    private final Conversor<Message, MessageDTO> conversor;

    public ExternalMsgController(SimpMessagingTemplate simpMessagingTemplate, NotificationService notificationService) {
        this.conversor = new Conversor<>(Message.class, MessageDTO.class);
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.notificationService = notificationService;
    }

    @PostMapping("/message")
    public void envia(@RequestBody MessageDTO messageDTO, Principal principal) {
        var message = notificationService.grava(messageDTO, principal);
        simpMessagingTemplate.convertAndSend("/topic/" + message.getUser().getEmail(), new OutputMessage(message.getId(), null, null, null, null));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<OutputMessage>> envia(Principal principal) throws JsonProcessingException {
        return new ResponseEntity<>(notificationService.get(principal), HttpStatus.OK);
    }

    @GetMapping("/message/{id}")
    public ResponseEntity<OutputMessage> get(Principal principal, @PathVariable Long id) throws JsonProcessingException {
        return new ResponseEntity<>(notificationService.get(principal, id), HttpStatus.OK);
    }
}

