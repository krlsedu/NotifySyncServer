package com.csctracker.notifysyncserver.websockets;

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
