package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Policy;

import java.util.List;

public interface PolicyService {

    List<Policy> getAllPolicies();

    Policy getPolicyById(Long id);

    Policy createPolicy(Policy policy);

    Policy updatePolicy(Long id, Policy policy);

    void deletePolicy(Long id);
}
