package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "PaymentMethods")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentMethodId;

    @Column(name = "method_type", columnDefinition = "NVARCHAR(20)")
    private String methodType;

    @Column(name = "provider", columnDefinition = "NVARCHAR(50)")
    private String provider;

    @Column(name = "account_no")
    private String accountNo;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @CreationTimestamp
    private  LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
