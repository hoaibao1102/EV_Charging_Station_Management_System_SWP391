package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.VehicleModelCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.VehicleModelUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.VehicleModelResponse;
import com.swp391.gr3.ev_management.service.VehicleModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicle-models")
@Tag(name = "Vehicle Model Controller", description = "APIs for managing vehicle models")
public class VehicleModelController {

    @Autowired
    private VehicleModelService vehicleModelService;

    // Alias create to match /create path expected by clients
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    @Operation(summary = "Create Vehicle Model", description = "Create a new vehicle model")
    public ResponseEntity<VehicleModelResponse> create(@Valid @RequestBody VehicleModelCreateRequest request) {
        return ResponseEntity.ok(vehicleModelService.create(request));
    }

    // List all (or search via query params)
    @GetMapping
    @Operation(summary = "List or Search Vehicle Models", description = "List all vehicle models or search by brand, model, year, or connector type")
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
    @Operation(summary = "Update Vehicle Model", description = "Update an existing vehicle model by its ID")
    public ResponseEntity<VehicleModelResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody VehicleModelUpdateRequest request) {
        return ResponseEntity.ok(vehicleModelService.update(id, request));
    }

    // Delete
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Vehicle Model", description = "Delete a vehicle model by its ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleModelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
