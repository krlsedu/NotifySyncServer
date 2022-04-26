package com.csctracker.notifysyncserver.dto;

import lombok.Data;

@Data
public class MessageDTO {
    private Long id;
    private String from;
    private String text;
    private String app;
    private String time;
    private NotificationSyncDTO notificationSyncDTO;
}
