package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "VehicleModels")
@Data
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vehicleModelId;

    @Column(name = "brand", length = 100, nullable = true)
    private String brand;

    @Column(name = "model_name", length = 100, nullable = true)
    private String modelName;

    @Column(name = "battery_capacity_kwh")
    private Double batteryCapacityKwh;

    @OneToMany(mappedBy = "vehiclemodel", cascade = CascadeType.ALL)
    private List<Vehicle> vehicles;

    @ManyToOne
    @JoinColumn(name = "ConnectorTypeId")
    private ConnectorType connectortype;
}
