package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.DriverStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverRequest {

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Trạng thái tài xế không được để trống")
    private DriverStatus driverStatus = DriverStatus.ACTIVE; // trạng thái mặc định khi tạo tài xế

    @NotNull(message = "ID tài xế không được để trống")
    @Positive(message = "ID tài xế phải là số dương")
    private Long driverId;

    @NotBlank(message = "Tên tài xế không được để trống")
    private String driverName;
}
