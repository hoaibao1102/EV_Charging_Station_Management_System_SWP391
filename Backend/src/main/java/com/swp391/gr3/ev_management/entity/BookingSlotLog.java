package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "BookingSlotLogs")
@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class BookingSlotLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingID")
    private Booking booking;

    @Column(name = "SlotIndex")
    private int slotIndex;

    @Column(name = "SlotDurationMin")
    private int slotDurationMin;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
