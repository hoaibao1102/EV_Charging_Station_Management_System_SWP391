package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Policy;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;

    @Override
    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    @Override
    public Policy getPolicyById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ErrorException("Policy not found"));
    }

    @Override
    public Policy createPolicy(Policy policy) {
        // Đảm bảo không set id từ client
        policy.setPolicyId(null);
        return policyRepository.save(policy);
    }

    @Override
    public Policy updatePolicy(Long id, Policy updatedPolicy) {
        Policy existing = policyRepository.findById(id)
                .orElseThrow(() -> new ErrorException("Policy not found"));

        existing.setPolicyName(updatedPolicy.getPolicyName());
        existing.setPolicyDescription(updatedPolicy.getPolicyDescription());

        return policyRepository.save(existing);
    }

    @Override
    public void deletePolicy(Long id) {
        Policy existing = policyRepository.findById(id)
                .orElseThrow(() -> new ErrorException("Policy not found"));

        policyRepository.delete(existing);
    }
}
