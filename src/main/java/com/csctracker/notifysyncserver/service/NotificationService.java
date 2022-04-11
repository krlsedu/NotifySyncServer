package com.csctracker.notifysyncserver.service;

import com.csctracker.notifysyncserver.dto.Conversor;
import com.csctracker.notifysyncserver.dto.Message;
import com.csctracker.notifysyncserver.dto.NotificationSync;
import com.csctracker.notifysyncserver.dto.OutputMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class NotificationService {

    private final ObjectMapper objectMapper;

    private final Conversor<Message, OutputMessage> conversor;

    public NotificationService() {
        objectMapper = new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        conversor = new Conversor<>(Message.class, OutputMessage.class);
    }

    public OutputMessage convertMessage(Message message) throws JsonProcessingException {
        NotificationSync notificationSync = objectMapper.readValue(message.getText(), NotificationSync.class);
        message.setFrom(notificationSync.getTitle());
        message.setText(notificationSync.getText());
        message.setApp(notificationSync.getAppName());
        message.setTime(new SimpleDateFormat("HH:mm:ss").format(new Date(notificationSync.getSystemTime())));
        return conversor.toT(message);
    }
}
