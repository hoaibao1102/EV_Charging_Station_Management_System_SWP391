package com.swp391.gr3.ev_management.entity;

import com.swp391.gr3.ev_management.enums.BookingStatus;
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
    @JoinColumn(name = "StationID", nullable = false)
    private ChargingStation station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleID", nullable = false)
    private UserVehicle vehicle;


    @Column(name = "BookingTime", nullable = false)
    private LocalDateTime bookingTime;

    @Column(name = "ScheduledStartTime", nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column(name = "ScheduledEndTime", nullable = false)
    private LocalDateTime scheduledEndTime;

    @Column(name = "Status", columnDefinition = "NVARCHAR(20)", nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "Notes", columnDefinition = "NVARCHAR(255)")
    private String notes;

    @CreationTimestamp
    @Column( name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private ChargingSession chargingSession;

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    private List<BookingSlot> bookingSlots;
}