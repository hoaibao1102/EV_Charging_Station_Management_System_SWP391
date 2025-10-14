package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "USERS")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Long userId;

    @Column(name = "Email", unique = true, nullable = false, columnDefinition = "NVARCHAR(255)")
    private String email;

    @Column(name = "Phone_number", unique = true, columnDefinition = "NVARCHAR(15)")
    private String phoneNumber;

    @Column(name = "Password_hash", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String passwordHash;

    @Column(name = "Name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String name;

    @Column(name = "Date_of_birth")
    private LocalDateTime dateOfBirth;

    @Column(name = "Gender", columnDefinition = "NVARCHAR(1)")
    private String gender;

    @Column(name = "Address", columnDefinition = "NVARCHAR(50)")
    private String address;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleID")
    private Role role;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Driver driver;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Admin admin;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<StationStaff> stationStaffs;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Notification> notifications;



}
