package com.swp391.gr3.ev_management.entity;

import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ChargingSession")
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SessionID")
    private Long sessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingID", unique = true, nullable = true)
    private Booking booking;

    @Column(name = "StartTime")
    private LocalDateTime startTime;

    @Column(name = "EndTime")
    private LocalDateTime endTime;

    @Column(name = "InitialSoc", nullable = false)
    private Integer initialSoc;

    @Column(name = "FinalSoc")
    private Integer finalSoc;

    @Column(name = "EnergyKWh")
    private double energyKWh;

    @Column(name = "DurationMinutes")
    private int durationMinutes;

    @Column(name = "Cost")
    private double cost;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", columnDefinition = "NVARCHAR(20)", nullable = false)
    private ChargingSessionStatus status;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "session", fetch = FetchType.LAZY)
    private Invoice invoice;

    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
    private List<Notification> notifications;
}
