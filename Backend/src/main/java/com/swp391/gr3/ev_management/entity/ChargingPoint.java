package com.swp391.gr3.ev_management.entity;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
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
    @JoinColumn(name = "StationID", nullable = false)
    private ChargingStation station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConnectorTypeID", nullable = false)
    private ConnectorType connectorType;

    @OneToMany(mappedBy = "chargingPoint", fetch = FetchType.LAZY)
    private List<SlotAvailability> slotAvailabilities;

    @Column(name = "PointNumber", columnDefinition = "NVARCHAR(20)", nullable = false, unique = true)
    private String pointNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", columnDefinition = "NVARCHAR(20)", nullable = false)
    private ChargingPointStatus status;

    @Column(name = "SerialNumber", columnDefinition = "NVARCHAR(100)", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "InstallationDate", nullable = false)
    private LocalDateTime installationDate;

    @Column(name = "LastMaintenanceDate")
    private LocalDateTime lastMaintenanceDate;

    @Column(name = "QRCode", columnDefinition = "NVARCHAR(255)")
    private String qrCode;

    @Column(name = "MaxPowerKW", nullable = false)
    private double maxPowerKW;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;
}
