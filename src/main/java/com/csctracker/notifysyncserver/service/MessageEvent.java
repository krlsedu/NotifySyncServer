package com.csctracker.notifysyncserver.service;

import com.csctracker.notifysyncserver.dto.OutputMessageDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class MessageEvent extends ApplicationEvent {

    @Getter
    private final OutputMessageDTO message;

    public MessageEvent(OutputMessageDTO message) {
        super(message);
        this.message = message;
    }

}
