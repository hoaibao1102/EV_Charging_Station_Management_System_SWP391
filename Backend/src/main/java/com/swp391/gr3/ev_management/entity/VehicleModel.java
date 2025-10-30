package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "VehicleModels")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ModelID")
    private Long modelId;

    @Column(name = "Brand", columnDefinition = "NVARCHAR(100)",nullable = false)
    private String brand;

    @Column(name = "Model", columnDefinition = "NVARCHAR(100)",nullable = false)
    private String model;

    @Column(name = "ImageURL", columnDefinition = "NVARCHAR(255)",nullable = false)
    private String imageUrl;

    @Column(name = "imagePublicId", columnDefinition = "NVARCHAR(255)",nullable = false)
    private String imagePublicId;

    @Column(name = "Year",nullable = false)
    private int year;

    @Column(name = "BatteryCapacityKWh", nullable = false)
    private double batteryCapacityKWh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConnectorTypeID",nullable = false)
    private ConnectorType connectorType;

    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY)
    private List<UserVehicle> userVehicles;
}
