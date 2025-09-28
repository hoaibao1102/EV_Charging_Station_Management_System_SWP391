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

    @Column(columnDefinition = "NVARCHAR(255)")
    private String description;

    @OneToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private List<Users> users;
}
