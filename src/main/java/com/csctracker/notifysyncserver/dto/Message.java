package com.csctracker.notifysyncserver.dto;

import lombok.Data;

@Data
public class Message {
    private String from;
    private String text;
    private String app;
    private String time;
    private NotificationSync notificationSync;
}
