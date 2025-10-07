package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table (name = "Incidents")
@Data @NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long incidentId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StationStaffID")
    private StationStaff stationStaff;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StationID")
    private ChargingStation station;


    @Column(name = "title", length = 255)
    private String title;


    @Column(name = "description", columnDefinition = "NVARCHAR(500)")
    private String description;


    @Column(name = "severity", columnDefinition = "NVARCHAR(20)")
    private String severity;


    @Column(name = "status", columnDefinition = "NVARCHAR(20)")
    private String status;


    @Column(name = "reported_at")
    private LocalDateTime reportedAt;


    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
