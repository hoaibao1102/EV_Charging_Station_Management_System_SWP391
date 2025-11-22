package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateVehicleRequest {

    @NotNull(message = "ID mẫu xe không được để trống")
    @Positive(message = "ID mẫu xe phải là số dương")
    private Long modelId;   // nếu đúng là optional thì bỏ @NotNull + @Positive

    @NotNull(message = "Biển số xe không được để trống")
    @Pattern(
            regexp = "^\\d{2}[A-Za-z]{1,2}\\d{4,5}$",
            message = "Biển số xe phải đúng cấu trúc VN: 2 số tỉnh + 1-2 chữ cái + 4-5 số (VD: 86B381052, 30G12345, 51AB12345)"
    )
    private String licensePlate;
}
