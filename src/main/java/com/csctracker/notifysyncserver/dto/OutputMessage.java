package com.csctracker.notifysyncserver.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutputMessage {
    private String from;
    private String text;
    private String time;
    private String app;
}
