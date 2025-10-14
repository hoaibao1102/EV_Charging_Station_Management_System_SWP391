package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.VehicleModelCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.VehicleModelUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.VehicleModelResponse;
import com.swp391.gr3.ev_management.service.VehicleModelService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle-models")
public class VehicleModelController {

    @Autowired
    private VehicleModelService vehicleModelService;

    // Create
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<VehicleModelResponse> create(@Valid @RequestBody VehicleModelCreateRequest request) {
        return ResponseEntity.ok(vehicleModelService.create(request));
    }

    // Alias create to match /create path expected by clients
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<VehicleModelResponse> createAlias(@Valid @RequestBody VehicleModelCreateRequest request) {
        return ResponseEntity.ok(vehicleModelService.create(request));
    }

    // Get by id
    @GetMapping("/{id}")
    public ResponseEntity<VehicleModelResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleModelService.getById(id));
    }

    // List all (or search via query params)
    @GetMapping
    public ResponseEntity<List<VehicleModelResponse>> listOrSearch(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, name = "connectorTypeId") Integer connectorTypeId
    ) {
        if (brand != null || model != null || year != null || connectorTypeId != null) {
            return ResponseEntity.ok(vehicleModelService.search(brand, model, year, connectorTypeId));
        }
        return ResponseEntity.ok(vehicleModelService.getAll());
    }

    // Update
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<VehicleModelResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody VehicleModelUpdateRequest request) {
        return ResponseEntity.ok(vehicleModelService.update(id, request));
    }

    // Delete
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleModelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
