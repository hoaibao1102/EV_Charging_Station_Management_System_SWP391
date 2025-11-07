package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ConnectorTypeCreateRequest;
import com.swp391.gr3.ev_management.dto.request.ConnectorTypeUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.ConnectorTypeResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConnectorTypeServiceImpl implements ConnectorTypeService {

    private final ConnectorTypeRepository connectorTypeRepository;

    @Override
    public List<ConnectorTypeResponse> getAllConnectorTypes() {
        return connectorTypeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ConnectorTypeResponse getConnectorTypeById(Long connectorTypeId) {
        ConnectorType connectorType = connectorTypeRepository.findById(connectorTypeId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + connectorTypeId));
        return toResponse(connectorType);
    }

    @Override
    @Transactional
    public ConnectorTypeResponse createConnectorType(ConnectorTypeCreateRequest request) {
        // Kiểm tra code đã tồn tại
        if (connectorTypeRepository.existsByCodeAndMode(request.getCode(), request.getMode())) {
            throw new ErrorException("Code and Mode không được trùng: " + request.getCode() + "And" + request.getMode());
        }

        ConnectorType connectorType = ConnectorType.builder()
                .code(request.getCode())
                .mode(request.getMode())
                .displayName(request.getDisplayName())
                .defaultMaxPowerKW(request.getDefaultMaxPowerKW())
                .isDeprecated(request.getIsDeprecated())
                .build();

        ConnectorType saved = connectorTypeRepository.save(connectorType);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ConnectorTypeResponse updateConnectorType(Long connectorTypeId, ConnectorTypeUpdateRequest request) {
        ConnectorType connectorType = connectorTypeRepository.findById(connectorTypeId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + connectorTypeId));

        // Kiểm tra code nếu muốn update và code khác với code hiện tại
        if (request.getCode() != null && !request.getCode().equals(connectorType.getCode())) {
            if (connectorTypeRepository.existsByCodeAndMode(request.getCode(), request.getMode())) {
                throw new ErrorException("Code and Mode không được trùng: " + request.getCode() + "And" + request.getMode());
            }
            connectorType.setCode(request.getCode());
        }

        // Update các field nếu không null
        if (request.getMode() != null) {
            connectorType.setMode(request.getMode());
        }
        if (request.getDisplayName() != null) {
            connectorType.setDisplayName(request.getDisplayName());
        }
        if (request.getDefaultMaxPowerKW() != 0) {
            connectorType.setDefaultMaxPowerKW(request.getDefaultMaxPowerKW());
        }
        if (request.getIsDeprecated() != null) {
            connectorType.setIsDeprecated(request.getIsDeprecated());
        }

        ConnectorType updated = connectorTypeRepository.save(connectorType);
        return toResponse(updated);
    }


    // Helper method to convert Entity to Response DTO
    private ConnectorTypeResponse toResponse(ConnectorType connectorType) {
        return ConnectorTypeResponse.builder()
                .connectorTypeId(connectorType.getConnectorTypeId())
                .code(connectorType.getCode())
                .mode(connectorType.getMode())
                .displayName(connectorType.getDisplayName())
                .defaultMaxPowerKW(connectorType.getDefaultMaxPowerKW())
                .isDeprecated(connectorType.getIsDeprecated())
                .build();
    }
}
