package com.csctracker.notifysyncserver.dto;

import lombok.Data;

import java.util.Date;

@Data
public class MessageDTO {
    private Long id;
    private String from;
    private String text;
    private String app;
    private String operation;
    private String time;
    private String uuid;
    private String data;
    private String machine;
    private Date dateSynced;
    private NotificationSyncDTO notificationSyncDTO;
}
