package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.service.SlotAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/slot-availability")
@RequiredArgsConstructor
@Tag(name = "Slot Availability", description = "APIs for managing slot availability")
public class SlotAvailabilityController {

    private final SlotAvailabilityService slotAvailabilityService;

    // Cập nhật trạng thái slot availability
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{slotAvailabilityId}/status")
    @Operation(summary = "Update slot availability status")
    public ResponseEntity<SlotAvailabilityResponse> updateStatus(
            @PathVariable Long slotAvailabilityId,
            @RequestParam SlotStatus status
    ) {
        return ResponseEntity.ok(slotAvailabilityService.updateStatus(slotAvailabilityId, status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all slot configurations")
    public ResponseEntity<List<SlotAvailabilityResponse>> getAll() {
        return ResponseEntity.ok(slotAvailabilityService.findAll());
    }

    @GetMapping("/{pointId}")
    @Operation(
            summary = "Get slot availability by pointId",
            description = "Retrieve all slot availability records for a specific charging point"
    )
    public ResponseEntity<List<SlotAvailabilityResponse>> getById(@PathVariable Long pointId) {
        List<SlotAvailabilityResponse> responses = slotAvailabilityService.findByPointId(pointId);

        if (responses.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.emptyList());
        }

        return ResponseEntity.ok(responses);
    }
}
