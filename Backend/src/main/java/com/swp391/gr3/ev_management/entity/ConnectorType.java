package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.print.attribute.standard.MediaSize;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ConnectorType")
@Data
public class ConnectorType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long connectorTypeId;

    @Column(name = "code", columnDefinition = "NVARCHAR(20)")
    private String code;

    @Column(name = "model", columnDefinition = "NVARCHAR(10)")
    private String model;

    @Column(name = "display_name", columnDefinition = "NVARCHAR(100)")
    private String displayName;

    @Column(name = "default_max_power_kw")
    private Double defaultMaxPowerKw;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "connectortype")
    private List<VehicleModel> vehicleModel;

    @OneToOne(mappedBy = "connectortype")
    private ChargingPoint chargingpoint;

    @OneToOne(mappedBy = "connectortype")
    private Tariffs tariffs;

}
