package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.processing.Pattern;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Users")
@Data
public class Users implements UserDetails {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long userId;

    @Column(length = 100, unique = true, nullable = true)
    private String email;

    @Column(name = "phone_number", length = 15, unique = true, nullable = true)
    private String phoneNumber;

    @Column(name = "password_hash", length = 255, nullable = true)
    private String password;

    @Column(name = "first_name", columnDefinition = "NVARCHAR(100)", nullable = true)
    private String firstName;

    @Column(name = "last_name", columnDefinition = "NVARCHAR(100)", nullable = true)
    private String lastName;

    @Column(name = "date_of_birth", nullable = true)
    private Date dateOfBirth;

    @Column(name = "gender", length = 100, nullable = true)
    private String gender;

    @Column(name = "address", length = 100, nullable = true)
    private String address;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "RoleId", nullable = false)
    private Roles roles;

    @OneToOne(mappedBy = "users")
    private Admin admin;

    @OneToMany(mappedBy = "users")
    private List<Notification> notifications;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return this.getPhoneNumber();
    }
}
