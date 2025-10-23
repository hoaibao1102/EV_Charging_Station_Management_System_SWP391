package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.SlotConfigRequest;
import com.swp391.gr3.ev_management.DTO.response.SlotConfigResponse;
import com.swp391.gr3.ev_management.service.SlotConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slot-configs")
@RequiredArgsConstructor
@Tag(name = "Slot Configuration", description = "APIs for managing slot configurations")
public class SlotConfigController {

    private final SlotConfigService slotConfigService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all slot configurations")
    public ResponseEntity<List<SlotConfigResponse>> getAll() {
        return ResponseEntity.ok(slotConfigService.findAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{configId}")
    @Operation(summary = "Get slot configuration by ID")
    public ResponseEntity<SlotConfigResponse> getById(@PathVariable Long configId) {
        SlotConfigResponse response = slotConfigService.findByConfigId(configId);
        if (response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/station/{stationId}")
    @Operation(summary = "Get slot configuration by Charging Station ID")
    public ResponseEntity<SlotConfigResponse> getByStation(@PathVariable Long stationId) {
        SlotConfigResponse response = slotConfigService.findByStation_StationId(stationId);
        if (response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Add a new slot configuration")
    public ResponseEntity<SlotConfigResponse> add(@RequestBody SlotConfigRequest req) {
        SlotConfigResponse created = slotConfigService.addSlotConfig(req);
        return ResponseEntity.ok(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{configId}")
    @Operation(summary = "Update an existing slot configuration")
    public ResponseEntity<SlotConfigResponse> update(@PathVariable Long configId, @RequestBody SlotConfigRequest req) {
        SlotConfigResponse updated = slotConfigService.updateSlotConfig(configId, req);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }
}
