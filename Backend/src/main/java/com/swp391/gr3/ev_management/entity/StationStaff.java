package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Station_Staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StationStaffID")
    private Long stationStaffId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    // Quan hệ nhiều-nhiều được biểu diễn qua bảng này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StaffId")
    private Staffs staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StationId")
    private ChargingStation station;
}
