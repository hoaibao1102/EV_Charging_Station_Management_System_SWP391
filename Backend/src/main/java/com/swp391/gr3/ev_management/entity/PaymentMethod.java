package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "PaymentMethods")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MethodID")
    private Long methodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "MethodType", columnDefinition = "NVARCHAR(50)", nullable = false)
    private PaymentType methodType;

    @Column(name = "ProviderName", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String provider;

    @Column(name = "AccountNo", columnDefinition = "NVARCHAR(100)" , nullable = false, unique = true)
    private String accountNo;

    @Column(name = "ExpiryDate")
    private LocalDateTime expiryDate;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    //

    @OneToMany(mappedBy = "paymentMethod", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DriverId", nullable = false)
    private Driver driver;
}