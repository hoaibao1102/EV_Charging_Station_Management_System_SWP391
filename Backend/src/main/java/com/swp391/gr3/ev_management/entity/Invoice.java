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
@Table(name = "Invoices")
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InvoiceID")
    private Long invoiceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SessionID", unique = true, nullable = false)
    private ChargingSession session;

    @Column(name = "Amount", nullable = false)
    private double amount;

    @Column(name = "Currency", columnDefinition = "NVARCHAR(10)", nullable = false)
    private String currency;

    @Column(name = "Status", columnDefinition = "NVARCHAR(20)", nullable = false)
    private String status;

    @Column(name = "IssuedAt", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "PaidAt")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    private List<Transaction> transactions;
}
