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

    @Column(name = "Code", length = 20)
    private String code;

    @Column(name = "Mode", length = 10)
    private String mode;

    @Column(name = "DisplayName", length = 100)
    private String displayName;

    @Column(name = "DefaultMaxPowerKW", precision = 5, scale = 2)
    private BigDecimal defaultMaxPowerKW;

    @Column(name = "IsDeprecated")
    private Boolean isDeprecated;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
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
