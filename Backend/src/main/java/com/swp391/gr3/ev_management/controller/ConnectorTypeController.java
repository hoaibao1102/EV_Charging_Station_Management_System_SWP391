package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.ConnectorTypeCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.ConnectorTypeUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.ConnectorTypeResponse;
import com.swp391.gr3.ev_management.service.ConnectorTypeService;
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
@RequestMapping("/api/connector-types")
@Tag(name = "Connector Type Controller", description = "APIs for managing connector types")
@RequiredArgsConstructor
public class ConnectorTypeController {

    private final ConnectorTypeService connectorTypeService;

    @PutMapping("/{connectorTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update connector type", description = "Admin only - Update an existing connector type")
    public ResponseEntity<ConnectorTypeResponse> updateConnectorType(
            @PathVariable Long connectorTypeId,
            @Valid @RequestBody ConnectorTypeUpdateRequest request) {
        ConnectorTypeResponse response = connectorTypeService.updateConnectorType(connectorTypeId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create new connector type", description = "Admin only - Create a new connector type")
    public ResponseEntity<ConnectorTypeResponse> createConnectorType(
            @Valid @RequestBody ConnectorTypeCreateRequest request) {
        ConnectorTypeResponse response = connectorTypeService.createConnectorType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all connector types", description = "Public endpoint to retrieve all connector types")
    public ResponseEntity<List<ConnectorTypeResponse>> getAllConnectorTypes() {
        List<ConnectorTypeResponse> list = connectorTypeService.getAllConnectorTypes();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{connectorTypeId}")
    @Operation(summary = "Get connector type by ID", description = "Public endpoint to retrieve a specific connector type")
    public ResponseEntity<ConnectorTypeResponse> getConnectorTypeById(@PathVariable Long connectorTypeId) {
        ConnectorTypeResponse response = connectorTypeService.getConnectorTypeById(connectorTypeId);
        return ResponseEntity.ok(response);
    }

}

