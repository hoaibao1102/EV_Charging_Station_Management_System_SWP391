package com.swp391.gr3.ev_management.entity;

import com.swp391.gr3.ev_management.enums.ViolationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "DriverViolation")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class DriverViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ViolationID")
    private Long violationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DriverID",nullable = false)
    private Driver driver;

    @CreationTimestamp
    @Column(name = "OccurredAt",nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", columnDefinition = "NVARCHAR(20)",nullable = false)
    private ViolationStatus status;

    @Column(name = "Description", columnDefinition = "NVARCHAR(255)")
    private String description;

//    @Column(name = "penalty_amount", precision = 19, scale = 2)
//    private BigDecimal penaltyAmount;

}