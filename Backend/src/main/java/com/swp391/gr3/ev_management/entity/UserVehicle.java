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
    @JoinColumn(name = "DriverID")
    private Driver driver;

    @Column(name = "VehiclePlate", length = 20, unique = true)
    private String vehiclePlate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ModelID")
    private VehicleModel model;

    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private List<Booking> bookings;
}
