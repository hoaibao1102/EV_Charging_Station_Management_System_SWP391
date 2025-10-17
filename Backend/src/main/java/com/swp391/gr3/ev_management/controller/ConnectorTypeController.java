package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.ConnectorTypeResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/connector-types")
@Tag(name = "Connector Type Controller", description = "APIs for managing connector types")
@RequiredArgsConstructor
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

