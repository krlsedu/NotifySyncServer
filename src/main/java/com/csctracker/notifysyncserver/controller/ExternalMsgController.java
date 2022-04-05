package com.csctracker.notifysyncserver.controller;

import com.csctracker.notifysyncserver.websockets.Message;
import com.csctracker.notifysyncserver.websockets.OutputMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class ExternalMsgController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public ExternalMsgController(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @PostMapping("/teste")
    public void envia(@RequestBody final Message message) {

        final String time = new SimpleDateFormat("HH:mm").format(new Date());
        simpMessagingTemplate.convertAndSend("/topic/messages",
                new OutputMessage(message.getFrom(), message.getText(), time, message.getApp()));

    }
}

