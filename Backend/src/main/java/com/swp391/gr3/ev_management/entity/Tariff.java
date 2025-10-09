package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Tariffs")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TariffID")
    private Long tariffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConnectorTypeID", nullable = false)
    private ConnectorType connectorType;

    @Column(name = "PricePerKWh", nullable = false)
    private double pricePerKWh;

    @Column(name = "Currency", columnDefinition = "NVARCHAR(10)", nullable = false)
    private String currency;

    @Column(name = "EffectiveFrom", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "EffectiveTo", nullable = false)
    private LocalDateTime effectiveTo;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

}
