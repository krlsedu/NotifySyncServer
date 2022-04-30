package com.csctracker.notifysyncserver.controller;

import com.csctracker.notifysyncserver.dto.MessageDTO;
import com.csctracker.notifysyncserver.dto.OutputMessage;
import com.csctracker.notifysyncserver.model.Message;
import com.csctracker.notifysyncserver.service.NotificationService;
import com.csctracker.securitycore.dto.Conversor;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
public class ExternalMsgController {

    private final NotificationService notificationService;

    private final Conversor<Message, MessageDTO> conversor;

    public ExternalMsgController(NotificationService notificationService) {
        this.conversor = new Conversor<>(Message.class, MessageDTO.class);
        this.notificationService = notificationService;
    }

    @PostMapping("/message")
    public void envia(@RequestBody MessageDTO messageDTO, Principal principal) {
        notificationService.grava(messageDTO, principal);
    }

    @GetMapping("/messages")
    public ResponseEntity<List<OutputMessage>> envia(Principal principal) throws JsonProcessingException {
        return new ResponseEntity<>(notificationService.get(principal), HttpStatus.OK);
    }

    @GetMapping("/message/{id}")
    public ResponseEntity<OutputMessage> get(Principal principal, @PathVariable String id) throws JsonProcessingException {
        return new ResponseEntity<>(notificationService.get(principal, id), HttpStatus.OK);
    }
}

