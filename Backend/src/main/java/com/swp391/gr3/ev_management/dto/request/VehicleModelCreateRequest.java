package com.swp391.gr3.ev_management.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.swp391.gr3.ev_management.enums.VehicleModelStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModelCreateRequest {

    @NotBlank(message = "Hãng xe không được để trống")
    private String brand;

    @NotBlank(message = "Tên mẫu xe không được để trống")
    private String model;

    @NotNull(message = "Năm sản xuất không được để trống")
    @Min(value = 1886, message = "Năm sản xuất phải hợp lệ")
    private Integer year;

    @NotBlank(message = "URL hình ảnh không được để trống")
    private String imageUrl;

    @NotBlank(message = "Image Public ID không được để trống")
    private String imagePublicId;

    @NotNull(message = "ID loại đầu nối không được để trống")
    @Positive(message = "ID loại đầu nối phải là số dương")
    private Long connectorTypeId;

    @NotNull(message = "Trạng thái không được để trống")
    private VehicleModelStatus status;

    @JsonAlias({"batteryCapacity", "battery-capacity"})
    @NotNull(message = "Dung lượng pin là bắt buộc")
    @DecimalMin(value = "0.0", inclusive = false, message = "Dung lượng pin phải lớn hơn 0")
    private Double batteryCapacityKWh;
}
