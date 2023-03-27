package com.csctracker.notifysyncserver.dto;

import com.csctracker.model.User;
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
    private Boolean force;
    private Date dateSynced;
    private NotificationSyncDTO notificationSyncDTO;
    private Date dateSent;
    private User user;
    private OutputMessageDTO outputMessageDTO;
}
