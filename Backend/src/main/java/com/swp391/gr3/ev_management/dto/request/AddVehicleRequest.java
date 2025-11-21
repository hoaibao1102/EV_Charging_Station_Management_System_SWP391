package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.UserVehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddVehicleRequest {

    @NotNull(message = "ID mẫu xe không được để trống")
    @Positive(message = "ID mẫu xe phải là số dương")
    private Long modelId;

    @NotBlank(message = "Biển số xe không được để trống")
    @Pattern(
            regexp = "^\\d{2}[A-Za-z]{1,2}\\d{4,5}$",
            message = "Biển số xe phải đúng cấu trúc biển số VN: 2 chữ số (tỉnh) + 1-2 chữ cái + 4-5 chữ số (VD: 86B381052, 30G12345, 51AB12345)"
    )
    private String licensePlate;

    private UserVehicleStatus vehicleStatus;
}
