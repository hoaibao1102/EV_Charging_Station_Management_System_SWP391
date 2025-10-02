package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ChargingPoints")
public class ChargingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pointId;

    @Column(name = "point_number", columnDefinition = "NVARCHAR(20)", nullable = true)
    private String pointNumber;

    @Column(name = "status", columnDefinition = "NVARCHAR(12)", nullable = true)
    private String status;

    @Column(name = "serial_number", columnDefinition = "NVARCHAR(64)", nullable = true)
    private String serialNumber;

    @Column(name = "installation_date")
    private LocalDateTime installationDate;

    @Column(name = "last_maintenance_date")
    private LocalDateTime lastMaintenanceDate;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "max_power_kw")
    private Double maxPowerKw;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "PointId")
    private List<Booking>  booking;

    @ManyToOne
    @JoinColumn(name = "StationId", nullable = false)
    private ChargingStation chargingstation;

    @OneToOne
    @JoinColumn(name = "ConnectorTypeId")
    private ConnectorType connectortype;
}
