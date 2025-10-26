package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.TariffCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.TariffUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.TariffResponse;
import com.swp391.gr3.ev_management.service.TariffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
@Tag(name = "Tariff Controller", description = "APIs for managing tariffs")
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create new tariff", description = "Admin only - Create a new tariff")
    public ResponseEntity<TariffResponse> createTariff(
            @Valid @RequestBody TariffCreateRequest request) {
        TariffResponse response = tariffService.createTariff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{tariffId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update tariff", description = "Admin only - Update an existing tariff")
    public ResponseEntity<TariffResponse> updateTariff(
            @PathVariable long tariffId,
            @Valid @RequestBody TariffUpdateRequest request) {
        TariffResponse response = tariffService.updateTariff(tariffId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all tariffs", description = "Public endpoint to retrieve all tariffs")
    public ResponseEntity<List<TariffResponse>> getAllTariffs() {
        List<TariffResponse> list = tariffService.getAllTariffs();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{tariffId}")
    @Operation(summary = "Get tariff by ID", description = "Public endpoint to retrieve a specific tariff")
    public ResponseEntity<TariffResponse> getTariffById(@PathVariable long tariffId) {
        TariffResponse response = tariffService.getTariffById(tariffId);
        return ResponseEntity.ok(response);
    }
}
