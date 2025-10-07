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

    @Column(name = "Brand", length = 100)
    private String brand;

    @Column(name = "Model", length = 100)
    private String model;

    @Column(name = "Year")
    private int year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConnectorTypeID")
    private ConnectorType connectorType;

    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY)
    private List<UserVehicle> userVehicles;
}
