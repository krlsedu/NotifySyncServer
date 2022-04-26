package com.csctracker.notifysyncserver.model;

import com.csctracker.securitycore.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
    private Long id;


    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "notify_from")
    private String from;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String text;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String app;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "notfy_time")
    private String time;
    private Date dateSynced;
    private Date dateSent;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}