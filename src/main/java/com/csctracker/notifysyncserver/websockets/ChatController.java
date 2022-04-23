package com.csctracker.notifysyncserver.websockets;

import com.csctracker.notifysyncserver.dto.MessageDTO;
import com.csctracker.notifysyncserver.dto.OutputMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
public class ChatController {

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public OutputMessage send(final MessageDTO messageDTO) {
        final String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        return new OutputMessage(messageDTO.getFrom(), messageDTO.getText(), time, messageDTO.getApp());
    }

}
