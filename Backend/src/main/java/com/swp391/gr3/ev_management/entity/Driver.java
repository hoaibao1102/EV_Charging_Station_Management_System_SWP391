package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Drivers")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DriverID")
    private Long driverId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", unique = true)
    private User user;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "LastActiveAt")
    private LocalDateTime lastActiveAt;

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<UserVehicle> vehicles;

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<DriverViolation> violations;
}
