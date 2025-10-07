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
    @Column(name = "PaymentMethodID")
    private Long paymentMethodId;

    @Column(name = "MethodType", length = 50)
    private String methodType;

    @Column(name = "Provider", length = 100)
    private String provider;

    @Column(name = "AccountNo", length = 100)
    private String accountNo;

    @Column(name = "ExpiryDate")
    private LocalDateTime expiryDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "paymentMethod", fetch = FetchType.LAZY)
    private List<Transaction> transactions;

}
