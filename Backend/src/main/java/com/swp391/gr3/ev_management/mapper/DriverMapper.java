package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.DriverResponse;
import com.swp391.gr3.ev_management.dto.response.VehicleResponse;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.UserVehicle;
import com.swp391.gr3.ev_management.entity.VehicleModel;
import com.swp391.gr3.ev_management.enums.UserVehicleStatus;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {

    public DriverResponse toDriverResponse(Driver driver) {
        return DriverResponse.builder()
                .driverId(driver.getDriverId())
                .userId(driver.getUser().getUserId())
                .email(driver.getUser().getEmail())
                .phoneNumber(driver.getUser().getPhoneNumber())
                .name(driver.getUser().getName())
                .address(driver.getUser().getAddress())
                .gender(driver.getUser().getGender())              // Giới tính
                .dateOfBirth(driver.getUser().getDateOfBirth())    // Ngày sinh
                .status(driver.getStatus())                        // Trạng thái: ACTIVE/BANNED/SUSPENDED
                .createdAt(driver.getUser().getCreatedAt())        // Ngày tạo
                .updatedAt(driver.getUser().getUpdatedAt())
                .build();
    }

    public VehicleResponse toVehicleResponse(UserVehicle vehicle) {
        VehicleModel model = vehicle.getModel();
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .driverId(vehicle.getDriver().getDriverId())
                .licensePlate(vehicle.getVehiclePlate())
                .modelId(model.getModelId())
                .modelName(model.getModel())
                .brand(model.getBrand())
                .vehicleStatus(UserVehicleStatus.ACTIVE)
                .connectorTypeName(model.getConnectorType() != null ?
                        model.getConnectorType().getDisplayName() : null)
                .build();
    }
}
