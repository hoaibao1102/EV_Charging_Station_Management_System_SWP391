package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Bookings")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @Column(name = "booking_time")
    private LocalDateTime bookingTime;

    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;

    @Column(name = "status", columnDefinition = "NVARCHAR(255)")
    private String status;

    @Column(name = "note", columnDefinition = "NVARCHAR(255)")
    private String note;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "booking")
    private ChargingSession chargingsession;

    @OneToMany(mappedBy = "booking")
    private List<Notification> notifications;

    @ManyToOne
    @JoinColumn(name = "StationId")
    private ChargingStation chargingstation;

    @ManyToOne
    @JoinColumn(name = "PointId")
    private ChargingPoint point;
}
