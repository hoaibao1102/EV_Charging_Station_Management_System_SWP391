package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Vehicles")
@Data
@NoArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "license_plate", columnDefinition = "NVARCHAR(50)", unique = true, nullable = false)
    private String licensePlate;

    @Column(name = "model", columnDefinition = "NVARCHAR(100)")
    private String model;

    @Column(name = "brand", columnDefinition = "NVARCHAR(100)")
    private String brand;

    @Column(name = "year")
    private Integer year;

    @Column(name = "battery_capacity")
    private Double batteryCapacity; // in kWh

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}