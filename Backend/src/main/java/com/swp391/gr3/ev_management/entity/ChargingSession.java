package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ChargingSession")
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "energy_kwh")
    private Double energyKwh;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "cost")
    private Double cost;

    @Column(name = "status", columnDefinition = "NVARCHAR(15)")
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "BookingId", nullable = false)
    private Booking booking;

    @OneToMany(mappedBy = "chargingsession")
    private List<Notification> notifications;
}
