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
@Table(name = "Transactions")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionID")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InvoiceID", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PaymentMethodID", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "Amount", nullable = false)
    private double amount;

    @Column(name = "Currency", columnDefinition = "NVARCHAR(10)", nullable = false)
    private String currency;

    @Column(name = "Status", columnDefinition = "NVARCHAR(20)", nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY)
    private List<Notification> notifications;
}
