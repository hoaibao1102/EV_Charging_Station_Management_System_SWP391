package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleID")
    private Long roleId;


    @Column(name = "RoleName", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String roleName; // ADMIN, STAFF, EV_DRIVER


    @Column(name = "Description", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String description;


    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<User> users;
}
