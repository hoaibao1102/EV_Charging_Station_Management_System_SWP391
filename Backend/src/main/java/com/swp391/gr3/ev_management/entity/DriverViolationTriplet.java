package com.swp391.gr3.ev_management.entity;

import com.swp391.gr3.ev_management.enums.TripletStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "DRIVER_VIOLATION_TRIPLET")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverViolationTriplet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripletId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DriverId", nullable = false)
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private TripletStatus status; // OPEN, CLOSED, PAID, CANCELED

    @Column(name = "CountInGroup", nullable = false)
    private int countInGroup;

    @Column(name = "TotalPenalty", nullable = false)
    private double totalPenalty;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "V1_ViolationId")
    private DriverViolation v1;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "V2_ViolationId")
    private DriverViolation v2;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "V3_ViolationId")
    private DriverViolation v3;

    private LocalDateTime windowStartAt;
    private LocalDateTime windowEndAt;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    public void addViolation(DriverViolation v) {
        if (countInGroup >= 3) throw new IllegalStateException("Triplet is full");
        if (countInGroup == 0) { this.v1 = v; this.windowStartAt = v.getOccurredAt(); }
        else if (countInGroup == 1) this.v2 = v;
        else this.v3 = v;
        this.countInGroup++;
        this.totalPenalty += v.getPenaltyAmount();
        if (countInGroup == 3) {
            this.status = TripletStatus.CLOSED;
            this.windowEndAt = v.getOccurredAt();
            this.closedAt = LocalDateTime.now();
        }
    }
}
