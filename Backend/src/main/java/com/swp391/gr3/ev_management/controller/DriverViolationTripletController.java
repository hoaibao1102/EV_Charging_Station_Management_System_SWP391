package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.DriverViolationTripletResponse;
import com.swp391.gr3.ev_management.service.DriverViolationTripletService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/triplets")
@RequiredArgsConstructor
public class DriverViolationTripletController {

    private final DriverViolationTripletService driverViolationTripletService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping("/all")
    @Operation(summary = "Get all driver violation triplets")
    public ResponseEntity<List<DriverViolationTripletResponse>> getAllTriplets() {
        return ResponseEntity.ok(driverViolationTripletService.getAllTriplets());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping("/by-phone")
    @Operation(summary = "Get driver violation triplets by user phone number")
    public ResponseEntity<List<DriverViolationTripletResponse>> getByPhone(
            @RequestParam String phoneNumber
    ) {
        return ResponseEntity.ok(driverViolationTripletService.getTripletsByUserPhone(phoneNumber));
    }

    @PutMapping("/{tripletId}/pay")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<DriverViolationTripletResponse> markTripletAsPaid(
            @PathVariable Long tripletId) {
        return ResponseEntity.ok(driverViolationTripletService.updateTripletStatusToPaid(tripletId));
    }

    @PutMapping("/{tripletId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriverViolationTripletResponse> markTripletAsCancel(
            @PathVariable Long tripletId) {
        return ResponseEntity.ok(driverViolationTripletService.updateTripletStatusToCanceled(tripletId));
    }
}