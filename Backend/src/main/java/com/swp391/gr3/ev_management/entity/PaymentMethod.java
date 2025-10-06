package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "PaymentMethods")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "method_id")
    private Long methodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false, length = 50)
    private PaymentType methodType;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "paymentMethod", cascade = CascadeType.ALL)
    private List<Transaction> transactions;
}