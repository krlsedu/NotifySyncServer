package com.csctracker.notifysyncserver.controller;

import com.csctracker.dto.Conversor;
import com.csctracker.notifysyncserver.dto.MessageDTO;
import com.csctracker.notifysyncserver.dto.OutputMessage;
import com.csctracker.notifysyncserver.model.Message;
import com.csctracker.notifysyncserver.service.MessageRecivedEventPublisher;
import com.csctracker.notifysyncserver.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.text.ParseException;
import java.util.List;

@RestController
public class ExternalMsgController {

    private final NotificationService notificationService;

    private final Conversor<Message, MessageDTO> conversor;

    private final MessageRecivedEventPublisher publisher;

    public ExternalMsgController(NotificationService notificationService, MessageRecivedEventPublisher publisher) {
        this.publisher = publisher;
        this.conversor = new Conversor<>(Message.class, MessageDTO.class);
        this.notificationService = notificationService;
    }

    @PostMapping("/message")
    @ResponseStatus(HttpStatus.CREATED)
    public void envia(@RequestBody MessageDTO messageDTO, Principal principal) {
        notificationService.grava(messageDTO, principal);
    }

    @GetMapping("/messages")
    public ResponseEntity<List<OutputMessage>> envia(Principal principal) throws JsonProcessingException {
        return new ResponseEntity<>(notificationService.get(principal), HttpStatus.OK);
    }

    @GetMapping(path = "/messages-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<OutputMessage> flux() throws JsonProcessingException {
        return notificationService.buscaTodos().concat(Flux.create(publisher)).log();
    }

    @GetMapping("/last-messages")
    public ResponseEntity<List<OutputMessage>> getLastMessages() throws JsonProcessingException {
        return new ResponseEntity<>(notificationService.getMessages(), HttpStatus.OK);
    }

    @GetMapping("/last-messages-date")
    public ResponseEntity<List<OutputMessage>> getLastMessagesDate(@RequestParam(name = "date", required = false) String date) throws JsonProcessingException, ParseException {
        return new ResponseEntity<>(notificationService.getMessagesDate(date), HttpStatus.OK);
    }

    @GetMapping("/message")
    public ResponseEntity<OutputMessage> get(Principal principal, @RequestParam(name = "id") String id) throws JsonProcessingException {
        return new ResponseEntity<>(notificationService.get(principal, id), HttpStatus.OK);
    }
}

