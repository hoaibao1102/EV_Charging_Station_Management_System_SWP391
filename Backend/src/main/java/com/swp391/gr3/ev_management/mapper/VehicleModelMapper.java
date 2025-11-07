package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.request.VehicleModelCreateRequest;
import com.swp391.gr3.ev_management.dto.request.VehicleModelUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.VehicleModelResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.VehicleModel;
import com.swp391.gr3.ev_management.enums.VehicleModelStatus;
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
                .imagePublicId(vm.getImagePublicId())
                .connectorTypeId(ct != null ? ct.getConnectorTypeId() : null)
                .connectorTypeCode(ct != null ? ct.getCode() : null)
                .status(vm.getStatus() != null ? vm.getStatus() : null)
                .connectorTypeDisplayName(ct != null ? ct.getDisplayName() : null)
                .connectorDefaultMaxPowerKW(ct != null ? ct.getDefaultMaxPowerKW() : 0)
                .batteryCapacityKWh(vm.getBatteryCapacityKWh())
                .build();
    }

    /* ===== Create mapping ===== */
    public VehicleModel toEntityForCreate(VehicleModelCreateRequest req, ConnectorType connectorType) {
        return VehicleModel.builder()
                .brand(req.getBrand())
                .model(req.getModel())
                .year(req.getYear())
                .imageUrl(req.getImageUrl())
                .imagePublicId(req.getImagePublicId())
                .connectorType(connectorType)
                .status(VehicleModelStatus.ACTIVE)
                .batteryCapacityKWh(req.getBatteryCapacityKWh())
                .build();
    }

    /* ===== Patch/Update mapping (in-place) ===== */
    public void applyUpdates(VehicleModel vm, VehicleModelUpdateRequest req, ConnectorType connectorTypeIfChanged) {
        if (req.getBrand() != null) vm.setBrand(req.getBrand());
        if (req.getModel() != null) vm.setModel(req.getModel());
        if (req.getYear() != null) vm.setYear(req.getYear());
        if (req.getImageUrl() != null) vm.setImageUrl(req.getImageUrl());
        if (req.getImagePublicId() != null) vm.setImagePublicId(req.getImagePublicId());
        if (req.getBatteryCapacityKWh() != null) vm.setBatteryCapacityKWh(req.getBatteryCapacityKWh());
        if (connectorTypeIfChanged != null) vm.setConnectorType(connectorTypeIfChanged);
    }
}
