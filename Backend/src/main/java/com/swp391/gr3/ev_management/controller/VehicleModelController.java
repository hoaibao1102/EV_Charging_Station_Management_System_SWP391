package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.VehicleModelCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.VehicleModelUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.VehicleModelResponse;
import com.swp391.gr3.ev_management.repository.VehicleModelRepository;
import com.swp391.gr3.ev_management.service.VehicleModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Vehicle Model Controller", description = "APIs for managing vehicle models")
@RequestMapping("/api/vehicle-models")
public class VehicleModelController {

    @Autowired
    private VehicleModelService vehicleModelService;

    @Autowired
    private VehicleModelRepository vehicleModelRepository;

    // ============ PUBLIC ENDPOINTS (no auth required) ============
    
    // Step 1: GET /api/vehicle-models/brands - lấy danh sách tất cả hãng xe
    @GetMapping("/list-brands")
    @Operation(
        summary = "Get all vehicle brands", 
        description = "Get all distinct vehicle brands (Step 1: User selects a brand)"
    )
    public ResponseEntity<List<String>> getAllBrands() {
        List<String> brands = vehicleModelRepository.findAll()
                .stream()
                .map(vm -> vm.getBrand())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(brands);
    }

    // Step 2: GET /api/vehicle-models?brand=Tesla - lấy models theo brand đã chọn
    @GetMapping
    @Operation(
        summary = "Get vehicle models by brand", 
        description = "Get all vehicle models for a specific brand (Step 2: After user clicks a brand)"
    )
    public ResponseEntity<List<VehicleModelResponse>> getModelsByBrand(
            @RequestParam(required = true) String brand
    ) {
        return ResponseEntity.ok(vehicleModelService.search(brand, null, null, null));
    }

    // ============ ADMIN ENDPOINTS (require ADMIN role) ============

    // POST /api/admin/vehicle-models/create
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create-model")
    @Operation(summary = "Create Vehicle Model (Admin)", description = "Create a new vehicle model", hidden = false)
    public ResponseEntity<VehicleModelResponse> create(@Valid @RequestBody VehicleModelCreateRequest request) {
        return ResponseEntity.ok(vehicleModelService.create(request));
    }

    // GET /api/admin/vehicle-models
    @PreAuthorize("hasRole('ADMIN') or hasRole('DRIVER')")
    @GetMapping("/admin/list-models")
    @Operation(summary = "List or Search Vehicle Models (Admin)", description = "Admin endpoint to list all vehicle models")
    public ResponseEntity<List<VehicleModelResponse>> listOrSearchAdmin() {
        return ResponseEntity.ok(vehicleModelService.getAll());
    }

    // PUT /api/admin/vehicle-models/{id}
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/edit/{id}")
    @Operation(summary = "Update Vehicle Model (Admin)", description = "Update an existing vehicle model by its ID")
    public ResponseEntity<VehicleModelResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody VehicleModelUpdateRequest request) {
        return ResponseEntity.ok(vehicleModelService.update(id, request));
    }

    // DELETE /api/admin/vehicle-models/{id}
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/delete/{id}")
    @Operation(summary = "Delete Vehicle Model (Admin)", description = "Delete a vehicle model by its ID", hidden = false)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleModelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
