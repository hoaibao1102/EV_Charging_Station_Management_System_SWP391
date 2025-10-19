package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.ViolationRequest;
import com.swp391.gr3.ev_management.DTO.response.ViolationResponse;
import com.swp391.gr3.ev_management.enums.ViolationStatus;
import com.swp391.gr3.ev_management.service.ViolationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
@Slf4j
public class ViolationController {
    private final ViolationService violationService;

    /**
     * Tạo violation mới (TỰ ĐỘNG ban nếu >= 3 vi phạm)
     * POST /api/violations/users/{userId}
     */
    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")  // Chỉ admin/staff mới tạo được violation
    public ResponseEntity<ViolationResponse> createViolation(
            @PathVariable Long userId,
            @Valid @RequestBody ViolationRequest request) {

        log.info("Received request to create violation for userId: {}", userId);
        ViolationResponse response = violationService.createViolation(userId, request);

        HttpStatus status = response.isDriverAutoBanned()
                ? HttpStatus.CREATED  // 201 - Created và đã ban
                : HttpStatus.CREATED; // 201 - Created

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Lấy tất cả vi phạm của driver
     * GET /api/violations/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ViolationResponse>> getViolations(@PathVariable Long userId) {
        log.info("Getting all violations for userId: {}", userId);
        List<ViolationResponse> violations = violationService.getViolationsByUserId(userId);
        return ResponseEntity.ok(violations);
    }

    /**
     * Lấy vi phạm theo status
     * GET /api/violations/users/{userId}/status/{status}
     */
    @GetMapping("/users/{userId}/status/{status}")
    public ResponseEntity<List<ViolationResponse>> getViolationsByStatus(
            @PathVariable Long userId,
            @PathVariable ViolationStatus status) {

        log.info("Getting violations for userId: {} with status: {}", userId, status);
        List<ViolationResponse> violations = violationService.getViolationsByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(violations);
    }

    /**
     * Đếm số vi phạm ACTIVE
     * GET /api/violations/users/{userId}/count
     */
    @GetMapping("/users/{userId}/count")
    public ResponseEntity<Integer> countActiveViolations(@PathVariable Long userId) {
        log.info("Counting active violations for userId: {}", userId);
        int count = violationService.countActiveViolations(userId);
        return ResponseEntity.ok(count);
    }
}
