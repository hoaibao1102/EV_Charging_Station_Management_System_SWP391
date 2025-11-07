package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.TariffResponse;
import com.swp391.gr3.ev_management.entity.Tariff;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TariffResponseMapper {

    public TariffResponse toResponse(Tariff t) {
        if (t == null) return null;
        return TariffResponse.builder()
                .tariffId(t.getTariffId())
                .connectorTypeId(t.getConnectorType().getConnectorTypeId())
                .connectorTypeCode(t.getConnectorType().getCode())
                .connectorTypeName(t.getConnectorType().getDisplayName())
                .pricePerKWh(t.getPricePerKWh())
                .pricePerMin(t.getPricePerMin())
                .currency(t.getCurrency())
                .effectiveFrom(t.getEffectiveFrom())
                .effectiveTo(t.getEffectiveTo())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    public List<TariffResponse> toResponseList(List<Tariff> tariffs) {
        return tariffs.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
