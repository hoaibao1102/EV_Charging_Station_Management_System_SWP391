package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.entity.Policy;
import com.swp391.gr3.ev_management.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    // GET /api/policies
    @GetMapping
    public ResponseEntity<List<Policy>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    // GET /api/policies/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Policy> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    // POST /api/policies
    @PostMapping
    public ResponseEntity<Policy> createPolicy(@RequestBody Policy policy) {
        Policy created = policyService.createPolicy(policy);
        return ResponseEntity.ok(created);
    }

    // PUT /api/policies/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Policy> updatePolicy(
            @PathVariable Long id,
            @RequestBody Policy policy
    ) {
        Policy updated = policyService.updatePolicy(id, policy);
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/policies/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
