package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Tariffs")
@Data
public class Tariffs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tariffId;

    @Column(name = "price_per_kwh")
    private Double pricePerKwh;

    @Column(name = "currency", columnDefinition = "NVARCHAR(200)")
    private String currency;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "ConnectorTypeId")
    private ConnectorType connectortype;

}
