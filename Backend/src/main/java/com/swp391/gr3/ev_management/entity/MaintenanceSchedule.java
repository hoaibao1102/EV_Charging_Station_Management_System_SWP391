package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "MaintenanceSchedule")
@Data @NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long maintenanceId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PointID")
    private ChargingPoint point;


    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;


    @Column(name = "maintenance_type", columnDefinition = "NVARCHAR(20)")
    private String maintenanceType; // Preventive, Corrective, Emergency


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignedtechnician")
    private User assignedTechnician; // FK to USERS


    @Column(name = "Status", columnDefinition = "NVARCHAR(20)")
    private String status;


    @Column(name = "next_maintenance_date")
    private LocalDateTime nextMaintenanceDate;


    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;


    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
