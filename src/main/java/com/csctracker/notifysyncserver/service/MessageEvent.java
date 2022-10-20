package com.csctracker.notifysyncserver.service;

import com.csctracker.notifysyncserver.dto.OutputMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class MessageEvent extends ApplicationEvent {

    @Getter
    private final OutputMessage message;

    public MessageEvent(OutputMessage message) {
        super(message);
        this.message = message;
    }

}
