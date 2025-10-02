package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "Notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notiId;

    @Column(name = "type", columnDefinition = "NVARCHAR(30)")
    private String type;

    @Column(name = "content", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "title", columnDefinition = "NVARCHAR(200)")
    private String title;

    @Column(name = "status", columnDefinition = "NVARCHAR(15)")
    private String status;

    @ManyToOne
    @JoinColumn(name = "BookingId", nullable = false)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "UserId", nullable = false)
    private Users users;

    @ManyToOne
    @JoinColumn(name = "TransactionId", nullable = false)
    private Transaction transactions;

    @ManyToOne
    @JoinColumn(name = "SessionId", nullable = false)
    private ChargingSession chargingsession;
}
