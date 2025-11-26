package com.swp391.gr3.ev_management.mapper;

import org.springframework.stereotype.Component;

import com.swp391.gr3.ev_management.dto.response.DriverResponse;
import com.swp391.gr3.ev_management.dto.response.VehicleResponse;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.UserVehicle;
import com.swp391.gr3.ev_management.entity.VehicleModel;

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
                .vehicleStatus(vehicle.getStatus()) // ✅ FIX: Lấy status thực từ entity (field name là 'status' không phải 'vehicleStatus')
                .connectorTypeName(model.getConnectorType() != null ?
                        model.getConnectorType().getDisplayName() : null)
                .batteryCapacityKWh(model.getBatteryCapacityKWh()) // ✅ Thêm dung lượng pin từ model
                .build();
    }
}
