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
    @JoinColumn(name = "ConnectorTypeID")
    private ConnectorType connectorType;

    @Column(name = "PricePerKWh")
    private double pricePerKWh;

    @Column(name = "Currency", length = 10)
    private String currency;

    @Column(name = "EffectiveFrom")
    private LocalDateTime effectiveFrom;

    @Column(name = "EffectiveTo")
    private LocalDateTime effectiveTo;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
