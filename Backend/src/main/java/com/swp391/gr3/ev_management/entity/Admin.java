package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Admin")
@Data
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminId;

    @Column(name = "role_level")
    private String roleLevel;

    @OneToOne
    @JoinColumn(name = "user_id")
    private Users users;
}
