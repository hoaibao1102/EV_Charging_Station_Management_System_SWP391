package com.swp391.gr3.ev_management.entity;

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
@Table(name = "Bookings")
@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookingID")
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StationID")
    private ChargingStation station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleID")
    private UserVehicle vehicle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SlotID", unique = true)
    private SlotAvailability slot;

    @Column(name = "BookingTime")
    private LocalDateTime bookingTime;

    @Column(name = "ScheduledStartTime")
    private LocalDateTime scheduledStartTime;

    @Column(name = "ScheduledEndTime")
    private LocalDateTime scheduledEndTime;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "Notes", columnDefinition = "NTEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private ChargingSession chargingSession;

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    private List<Notification> notifications;
}
