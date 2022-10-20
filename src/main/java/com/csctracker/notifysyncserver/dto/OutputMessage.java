package com.csctracker.notifysyncserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutputMessage {

    private String uuid;
    private String from;
    private String text;
    private String time;
    private String app;
    private String operation;
    private String data;
    private String machine;
    private Date dateSynced;

    public String getId() {
        return uuid;
    }
}
