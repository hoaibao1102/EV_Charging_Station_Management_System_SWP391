package com.swp391.gr3.ev_management.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notifications", indexes = {
        @Index(name = "ix_notifications_userid_createdat", columnList = "UserID, CreatedAt")
})
@Data
public class Notification {

    public static final String STATUS_UNREAD = "UNREAD";
    public static final String STATUS_READ   = "READ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotiID")
    private Long notiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    @JsonIgnoreProperties({"notifications"}) // 👈 tránh vòng lặp ngược
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "BookingID")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "SessionID")
    private ChargingSession session;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "TransactionID")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "Type", columnDefinition = "NVARCHAR(50)", nullable = false)
    private NotificationTypes type; // ✅ Đúng

    @Column(name = "Title", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String title;

    @Column(name = "ContentNoti", columnDefinition = "NVARCHAR(1000)", nullable = false)
    private String contentNoti;

    @Column(name = "Status", columnDefinition = "NVARCHAR(20)", nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ReadAt")
    private LocalDateTime readAt;

    @PrePersist
    public void prePersist() {
        if (status == null || status.isBlank()) status = STATUS_UNREAD;
    }
}
