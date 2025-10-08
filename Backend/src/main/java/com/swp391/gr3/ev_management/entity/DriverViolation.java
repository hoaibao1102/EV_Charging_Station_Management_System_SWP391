package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "OccurredAt",nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "Status", columnDefinition = "NVARCHAR(20)",nullable = false)
    private String status;

    @Column(name = "Description", columnDefinition = "NVARCHAR(255)")
    private String description;
}
