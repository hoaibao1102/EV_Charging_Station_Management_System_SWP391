package com.swp391.gr3.ev_management.entity;

import com.swp391.gr3.ev_management.enums.StaffStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "STAFFS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staffs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StaffId")
    private Long staffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private StaffStatus status;

    @Column(name = "RoleAtStation", length = 30)
    private String roleAtStation;

    // Quan hệ tới User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID")
    private User user;

    // Quan hệ với Incident (1-nhiều)
    @OneToMany(mappedBy = "staffs", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Incident> incidents;

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<StationStaff> stationStaffs;
}
