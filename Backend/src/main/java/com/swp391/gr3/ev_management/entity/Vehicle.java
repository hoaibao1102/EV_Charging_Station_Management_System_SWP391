package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Vehicles")
@Data
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vehicleId;

    @Column(name = "license_plate", length = 20, nullable = false, unique = true)
    private String licensePlate;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "year")
    private Integer year;

    @ManyToOne
    @JoinColumn(name = "VehicleModelId", nullable = false)
    private VehicleModel vehiclemodel;
}
