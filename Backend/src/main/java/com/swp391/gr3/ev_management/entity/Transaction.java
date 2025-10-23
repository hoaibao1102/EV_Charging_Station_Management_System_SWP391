package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionID")
    private Long transactionId;

    @Column(name = "Amount", nullable = false) //, precision = 19, scale = 2 (cái này làm cái gì )
    private double amount;//TODO: nên double hay BigDecimal ?

    @Column(name = "Currency", columnDefinition = "NVARCHAR(10)", nullable = false)
    private String currency;

    @Column(name = "Description", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String description;

    //TODO: cần type ko?? ví dụ:  TopUp, Payment, Refund, SubscriptionFee
//    @Column(name = "type", columnDefinition = "NVARCHAR(50)", nullable = false)
//    private String type; // TopUp, Payment, Refund, SubscriptionFee

    @Column(name = "Status", columnDefinition = "NVARCHAR(20)", nullable = false)
    private String status; // Pending, Completed, Failed, Cancelled TODO: create Enum clas for status's Transaction

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    //

    //TODO: transaction có quan hệ với Driver hông ??
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DriverID", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InvoiceID", nullable = false)
    private Invoice invoice; // nullable - not all transactions are invoice payments why? vậy sao thống kê cuối tháng để cho phân tích nhỉ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MethodID", nullable = false)
    private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY)
    private List<Notification> notifications;
}