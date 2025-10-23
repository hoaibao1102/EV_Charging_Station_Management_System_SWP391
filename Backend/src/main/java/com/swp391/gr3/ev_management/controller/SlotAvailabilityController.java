package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.SlotAvailabilityCreateRequest;
import com.swp391.gr3.ev_management.DTO.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.service.SlotAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slot-availability")
@RequiredArgsConstructor
@Tag(name = "Slot Availability", description = "APIs for managing slot availability")
public class SlotAvailabilityController {

    private final SlotAvailabilityService slotAvailabilityService;

    // Tạo availability cho danh sách template + connector types chỉ định
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Create slot availability for specified templates and connector types")
    public ResponseEntity<List<SlotAvailabilityResponse>> create(@RequestBody SlotAvailabilityCreateRequest req) {
        return ResponseEntity.ok(slotAvailabilityService.createForTemplates(req));
    }

    // Tạo availability cho TẤT CẢ templates trong NGÀY (của 1 config) và tất cả ConnectorTypes của station
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate/daily")
    @Operation(summary = "Create slot availability for all templates of a config for today")
    public ResponseEntity<List<SlotAvailabilityResponse>> createForConfigToday(@RequestParam Long configId) {
        LocalDate today = LocalDate.now();
        return ResponseEntity.ok(slotAvailabilityService.createForConfigInDate(configId, today));
    }

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

    @GetMapping("/{slotAvailabilityId}")
    @Operation(summary = "Get slot availability by ID", description = "Retrieve slot availability details by its ID")
    public ResponseEntity<SlotAvailabilityResponse> getById(@PathVariable Long slotAvailabilityId) {
        SlotAvailabilityResponse response = slotAvailabilityService.findById(slotAvailabilityId);
        if (response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }
}
