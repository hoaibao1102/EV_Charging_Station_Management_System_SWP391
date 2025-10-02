package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency", columnDefinition = "NVARCHAR(10)")
    private String currency;

    @Column(name = "status", columnDefinition = "NVARCHAR(15)")
    private String status;

    @CreationTimestamp
    private LocalDateTime createAt;

    @UpdateTimestamp
    private LocalDateTime updateAt;

    @OneToMany(mappedBy = "transactions")
    private List<Notification> notifications;
}
