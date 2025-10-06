package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping("/user/{id}/upgrade-to-driver")
    @Operation(summary = "Upgrade user to driver",
            description = "Creates a Driver record and auto-initializes wallet with balance 0")
    public ResponseEntity<DriverResponse> UpgradeDriver(@PathVariable("id") Long idDriver,@Valid @RequestBody DriverRequest request) throws ChangeSetPersister.NotFoundException {
        DriverResponse response = driverService.upgradeToDriver(idDriver, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
