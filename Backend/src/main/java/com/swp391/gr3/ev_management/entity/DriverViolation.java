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
    @JoinColumn(name = "DriverID")
    private Driver driver;

    @Column(name = "OccurredAt")
    private LocalDateTime occurredAt;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;
}
