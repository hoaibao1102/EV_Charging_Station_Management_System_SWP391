package com.swp391.gr3.ev_management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Driver entity - Extends Users with driver-specific attributes.
 * Uses @MapsId for shared PK with Users (driverId = userId).
 */
@Entity
@Table(name = "Drivers")
@Data
@NoArgsConstructor
public class Driver {

    @Id
    @Column(name = "driver_id")
    private Long driverId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // driverId = userId (shared PK)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus driverStatus = DriverStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToOne(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DriverWallet wallet;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Vehicle> vehicles = new ArrayList<>();

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<Invoice> invoices = new ArrayList<>();

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<DriverViolation> violations = new ArrayList<>();

    // Thêm vào
    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    private List<PaymentMethod> paymentMethods;
}
