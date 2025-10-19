package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.DTO.response.VehicleResponse;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.UserVehicle;
import com.swp391.gr3.ev_management.entity.VehicleModel;
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
                .status(driver.getStatus())
                .address(driver.getUser().getAddress())
                .createdAt(driver.getUser().getCreatedAt())
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
                .connectorTypeName(model.getConnectorType() != null ?
                        model.getConnectorType().getDisplayName() : null)
                .build();
    }

    // Định dạng hiển thị đơn giản: chèn dấu '-' giữa block khu vực (2-3 ký tự đầu) và phần còn lại.
    // Vì lưu trữ đã normalized (A-Z0-9), formatter cố gắng để người dùng dễ đọc hơn.
    private String formatPlateForDisplay(String plate) {
    if (plate == null) return null;
    String p = plate.trim();
    if (p.length() <= 3) return p;
    // Heuristic: 2 hoặc 3 ký tự đầu là mã tỉnh + series, phần sau là số
    int split = Character.isDigit(p.charAt(2)) ? 2 : 3;
    if (p.length() <= split) return p;
    return p.substring(0, split) + "-" + p.substring(split);
    }
}
