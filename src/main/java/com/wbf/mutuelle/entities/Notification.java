package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String msg;

    @Temporal(TemporalType.TIMESTAMP)
    private Date send_date;

    @Temporal(TemporalType.TIMESTAMP)
    private Date event_date;

    private String receiver;
    private String phone;
    private String role;
}