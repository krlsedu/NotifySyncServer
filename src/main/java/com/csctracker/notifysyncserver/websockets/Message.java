package com.csctracker.notifysyncserver.websockets;

public class Message {

    private String from;
    private String text;

    private String app;

    public String getText() {
        return text;
    }

    public String getFrom() {
        return from;
    }

    public String getApp() {
        return app;
    }
}
