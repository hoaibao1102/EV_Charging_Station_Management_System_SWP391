package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Admin")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AdminID")
    private Long adminId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", unique = true,nullable = false)
    private User user;

    @Column(name = "RoleLevel", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String roleLevel;

    @CreationTimestamp
    @Column(name = "CreatedAt",nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt",nullable = false)
    private LocalDateTime updatedAt;
}
