package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.ConnectorTypeResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connector-types")
public class ConnectorTypeController {

    @Autowired
    private ConnectorTypeRepository connectorTypeRepository;

    @GetMapping
    public ResponseEntity<List<ConnectorTypeResponse>> getAll() {
        List<ConnectorTypeResponse> list = connectorTypeRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConnectorTypeResponse> getById(@PathVariable Integer id) {
        ConnectorType ct = connectorTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ConnectorType not found with id " + id));
        return ResponseEntity.ok(toDto(ct));
    }

    private ConnectorTypeResponse toDto(ConnectorType ct) {
        return ConnectorTypeResponse.builder()
                .connectorTypeId(ct.getConnectorTypeId())
                .code(ct.getCode())
                .mode(ct.getMode())
                .displayName(ct.getDisplayName())
                .defaultMaxPowerKW(ct.getDefaultMaxPowerKW())
                .isDeprecated(ct.getIsDeprecated())
                .build();
    }
}

