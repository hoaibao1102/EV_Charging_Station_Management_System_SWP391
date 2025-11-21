package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    @Column(name = "policy_name", nullable = false, columnDefinition = "NVARCHAR(40)")
    private String policyName;

    @Column(name = "policy_description", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String policyDescription;
}
