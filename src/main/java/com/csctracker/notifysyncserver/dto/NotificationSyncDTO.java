package com.csctracker.notifysyncserver.dto;

import lombok.Data;

@Data
public class NotificationSyncDTO {
    private String appName;
    private String text;
    private String title;
    private long systemTime;
}
