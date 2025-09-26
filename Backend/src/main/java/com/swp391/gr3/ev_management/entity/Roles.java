package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "Roles")
@Data
public class Roles {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long roleId;

    @Column(length = 20, unique = true, nullable = false)
    private String roleName;

    private String description;

    @OneToMany(mappedBy = "roles", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Users> users;
}
