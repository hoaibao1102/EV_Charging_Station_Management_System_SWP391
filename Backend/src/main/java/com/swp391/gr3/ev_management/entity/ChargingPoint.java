package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ChargingPoints")
@Data
@NoArgsConstructor
@AllArgsConstructor @Builder
public class ChargingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PointID")
    private Long pointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StationID")
    private ChargingStation station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConnectorTypeID")
    private ConnectorType connectorType;

    @Column(name = "PointNumber", length = 20)
    private String pointNumber;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "SerialNumber", length = 100)
    private String serialNumber;

    @Column(name = "InstallationDate")
    private LocalDateTime installationDate;

    @Column(name = "LastMaintenanceDate")
    private LocalDateTime lastMaintenanceDate;

    @Column(name = "QRCode", length = 255)
    private String qrCode;

    @Column(name = "MaxPowerKW")
    private double maxPowerKW;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
