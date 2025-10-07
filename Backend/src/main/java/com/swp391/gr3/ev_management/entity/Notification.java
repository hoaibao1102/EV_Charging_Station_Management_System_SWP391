package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotiID")
    private Long notiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingID")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SessionID")
    private ChargingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TransactionID")
    private Transaction transaction;

    @Column(name = "Type", length = 50)
    private String type;

    @Column(name = "Title", length = 255)
    private String title;

    @Column(name = "ContentNoti", columnDefinition = "NTEXT")
    private String contentNoti;

    @Column(name = "Status", length = 20)
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "ReadAt")
    private LocalDateTime readAt;
}
