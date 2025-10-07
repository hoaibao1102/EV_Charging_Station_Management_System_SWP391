package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ROLES")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleID")
    private Long roleId;


    @Column(name = "RoleName", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String roleName; // ADMIN, STAFF, EV_DRIVER


    @Column(name = "Description", columnDefinition = "NTEXT")
    private String description;


    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> users = new ArrayList<>();
}
