package com.swp391.gr3.ev_management.entity;

import com.swp391.gr3.ev_management.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table (name = "Reports")
@Data @NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long incidentId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StaffId")
    private Staffs staffs;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StationID", nullable = false)
    private ChargingStation station;


    @Column(name = "title", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String title;


    @Column(name = "description", columnDefinition = "NVARCHAR(500)")
    private String description;


    @Column(name = "severity", columnDefinition = "NVARCHAR(20)", nullable = false)
    private String severity;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "NVARCHAR(20)", nullable = false)
    private ReportStatus status;


    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;


    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
