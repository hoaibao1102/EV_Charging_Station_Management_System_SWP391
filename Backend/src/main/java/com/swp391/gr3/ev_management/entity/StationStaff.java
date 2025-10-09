package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "StationStaffs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationStaff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stationStaffId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stationID", nullable = false)
    private ChargingStation station;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;


    @Column(name = "status", columnDefinition = "NVARCHAR(20)", nullable = false)
    private String status;


    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;


    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;


    @OneToMany(mappedBy = "stationStaff", fetch = FetchType.LAZY)
    private List<Incident> incidents;
}
