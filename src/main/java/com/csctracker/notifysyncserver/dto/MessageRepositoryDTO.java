package com.csctracker.notifysyncserver.dto;

import com.csctracker.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class MessageRepositoryDTO {
    private Long id;
    private String notify_from;
    private String text;

    private String app;
    private String operation;
    private String data;

    private String notfy_time;
    private Date date_synced;
    private Date date_sent;

    private String uuid;
    private String request_id;
    private Long user_id;
}
