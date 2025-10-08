package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.print.attribute.standard.MediaSize;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ConnectorType")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class ConnectorType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ConnectorTypeID")
    private Integer connectorTypeId;

    @Column(name = "Code", columnDefinition = "NVARCHAR(20)", unique = true, nullable = false)
    private String code;

    @Column(name = "Mode", columnDefinition = "NVARCHAR(10)", nullable = false)
    private String mode;

    @Column(name = "DisplayName", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String displayName;

    @Column(name = "DefaultMaxPowerKW", nullable = false)
    private double defaultMaxPowerKW;

    @Column(name = "IsDeprecated", nullable = false)
    private Boolean isDeprecated;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "connectorType", fetch = FetchType.LAZY)
    private List<ChargingPoint> chargingPoints;

    @OneToMany(mappedBy = "connectorType", fetch = FetchType.LAZY)
    private List<VehicleModel> vehicleModels;

    @OneToMany(mappedBy = "connectorType", fetch = FetchType.LAZY)
    private List<SlotAvailability> slotAvailabilities;

    @OneToMany(mappedBy = "connectorType", fetch = FetchType.LAZY)
    private List<Tariff> tariffs;

}
