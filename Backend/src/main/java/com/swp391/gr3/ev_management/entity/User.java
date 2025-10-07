package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
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

    @Column(name = "Email", length = 255, unique = true)
    private String email;

    @Column(name = "Phone_number", length = 15, unique = true)
    private String phoneNumber;

    @Column(name = "Password_hash", length = 255)
    private String passwordHash;

    @Column(name = "Name", length = 100)
    private String name;

    @Column(name = "Date_of_birth")
    private LocalDateTime dateOfBirth;

    @Column(name = "Gender", length = 1)
    private String gender;

    @Column(name = "Address", length = 500)
    private String address;

    @CreationTimestamp
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
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
