package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.VehicleModelResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.VehicleModel;
import org.springframework.stereotype.Component;

@Component
public class VehicleModelMapper {

    public VehicleModelResponse toResponse(VehicleModel vm) {
        ConnectorType ct = vm.getConnectorType();
        return VehicleModelResponse.builder()
                .modelId(vm.getModelId())
                .brand(vm.getBrand())
                .model(vm.getModel())
                .year(vm.getYear())
                .imageUrl(vm.getImageUrl())
                .connectorTypeId(ct != null ? ct.getConnectorTypeId() : null)
                .connectorTypeCode(ct != null ? ct.getCode() : null)
                .connectorTypeDisplayName(ct != null ? ct.getDisplayName() : null)
                .connectorDefaultMaxPowerKW(ct != null ? ct.getDefaultMaxPowerKW() : 0)
                .batteryCapacityKWh(vm.getBatteryCapacityKWh())
                .build();
    }
}
