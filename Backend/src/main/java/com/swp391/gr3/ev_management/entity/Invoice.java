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
    @JoinColumn(name = "SessionID", unique = true)
    private ChargingSession session;

    @Column(name = "Amount")
    private double amount;

    @Column(name = "Currency", length = 10)
    private String currency;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "IssuedAt")
    private LocalDateTime issuedAt;

    @Column(name = "PaidAt")
    private LocalDateTime paidAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    private List<Transaction> transactions;
}
