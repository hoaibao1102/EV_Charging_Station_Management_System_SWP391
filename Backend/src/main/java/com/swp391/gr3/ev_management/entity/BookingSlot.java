package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "BookingSlots")
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class BookingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookingSlotID")
    private Long bookingSlotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingID", nullable = false)
    private Booking booking;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SlotID", nullable = false, unique = true) // UNIQUE!!!
    private SlotAvailability slot;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
