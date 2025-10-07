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
    @JoinColumn(name = "UserID", unique = true)
    private User user;

    @Column(name = "RoleLevel", length = 50)
    private String roleLevel;

    @CreationTimestamp
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}
