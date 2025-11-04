package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.TariffCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.TariffUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.TariffResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.Tariff;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import com.swp391.gr3.ev_management.repository.TariffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TariffServiceImpl implements TariffService {

    private final TariffRepository tariffRepository;
    private final ConnectorTypeRepository connectorTypeRepository;

    @Override
    public List<TariffResponse> getAllTariffs() {
        return tariffRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TariffResponse getTariffById(long tariffId) {
        Tariff tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy tariff với ID: " + tariffId));
        return toResponse(tariff);
    }

    @Override
    @Transactional
    public TariffResponse createTariff(TariffCreateRequest request) {
        // Validate effectiveFrom < effectiveTo
        if (request.getEffectiveFrom().isAfter(request.getEffectiveTo())) {
            throw new ErrorException("EffectiveFrom phải trước EffectiveTo");
        }

        // Kiểm tra ConnectorType có tồn tại không
        ConnectorType connectorType = connectorTypeRepository.findById(request.getConnectorTypeId())
                .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + request.getConnectorTypeId()));

        Tariff tariff = Tariff.builder()
                .connectorType(connectorType)
                .pricePerKWh(request.getPricePerKWh())
                .pricePerMin(request.getPricePerMin())
                .currency(request.getCurrency())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();

        Tariff saved = tariffRepository.save(tariff);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TariffResponse updateTariff(long tariffId, TariffUpdateRequest request) {
        Tariff tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy tariff với ID: " + tariffId));

        // Update ConnectorType nếu có
        if (request.getConnectorTypeId() > 0) {
            ConnectorType connectorType = connectorTypeRepository.findById(request.getConnectorTypeId())
                    .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + request.getConnectorTypeId()));
            tariff.setConnectorType(connectorType);
        }

        // Update các field nếu không null/0
        if (request.getPricePerKWh() > 0) {
            tariff.setPricePerKWh(request.getPricePerKWh());
        } else {
            tariff.setPricePerKWh(tariff.getPricePerKWh());
        }

        if (request.getPricePerMin() > 0) {
            tariff.setPricePerMin(request.getPricePerMin());
        } else {
            tariff.setPricePerMin(tariff.getPricePerMin());
        }

        if (request.getCurrency() != null) {
            tariff.setCurrency(request.getCurrency());
        } else {
            tariff.setCurrency(tariff.getCurrency());
        }

        if (request.getEffectiveFrom() != null) {
            tariff.setEffectiveFrom(request.getEffectiveFrom());
        } else {
            tariff.setEffectiveFrom(tariff.getEffectiveFrom());
        }

        if (request.getEffectiveTo() != null) {
            tariff.setEffectiveTo(request.getEffectiveTo());
        } else {
            tariff.setEffectiveTo(tariff.getEffectiveTo());
        }

        // Validate effectiveFrom < effectiveTo sau khi update
        if (tariff.getEffectiveFrom().isAfter(tariff.getEffectiveTo())) {
            throw new ErrorException("EffectiveFrom phải trước EffectiveTo");
        }

        Tariff updated = tariffRepository.save(tariff);
        return toResponse(updated);
    }

    // Helper method to convert Entity to Response DTO
    private TariffResponse toResponse(Tariff tariff) {
        return TariffResponse.builder()
                .tariffId(tariff.getTariffId())
                .connectorTypeId(tariff.getConnectorType().getConnectorTypeId())
                .connectorTypeCode(tariff.getConnectorType().getCode())
                .connectorTypeName(tariff.getConnectorType().getDisplayName())
                .pricePerKWh(tariff.getPricePerKWh())
                .pricePerMin(tariff.getPricePerMin())
                .currency(tariff.getCurrency())
                .effectiveFrom(tariff.getEffectiveFrom())
                .effectiveTo(tariff.getEffectiveTo())
                .createdAt(tariff.getCreatedAt())
                .updatedAt(tariff.getUpdatedAt())
                .build();
    }
}
