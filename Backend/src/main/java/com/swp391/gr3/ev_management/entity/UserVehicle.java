package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "Vehicles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VehicleID")
    private Long vehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DriverID",nullable = false)
    private Driver driver;

    @Column(name = "VehiclePlate", unique = true, nullable = false, columnDefinition = "NVARCHAR(20)")
    private String vehiclePlate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ModelID",nullable = false)
    private VehicleModel model;

    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private List<Booking> bookings;
}
