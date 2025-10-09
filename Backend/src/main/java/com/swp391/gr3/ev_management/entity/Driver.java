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
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DriverID")
    private Long driverId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", unique = true, nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private DriverStatus Status = DriverStatus.PENDING;


    @Column(name = "LastActiveAt")
    private LocalDateTime lastActiveAt;

    // Relationships
//    @OneToOne(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//    private DriverWallet wallet;

    @OneToMany(mappedBy = "driver",cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<UserVehicle> vehicles;

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<DriverViolation> violations;

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<Invoice> invoices = new ArrayList<>();

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
}
