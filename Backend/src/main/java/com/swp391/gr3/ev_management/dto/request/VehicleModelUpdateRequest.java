package com.swp391.gr3.ev_management.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleModelUpdateRequest {

    @NotNull(message = "Hãng xe không được để trống")
    private String brand; // optional

    @NotNull(message = "Tên mẫu xe không được để trống")
    private String model; // optional

    @NotNull(message = "Năm sản xuất không được để trống")
    @Min(value = 1886, message = "Năm sản xuất phải hợp lệ")
    @Positive(message = "Năm sản xuất phải là số dương")
    private Integer year; // optional

    @NotNull(message = "URL hình ảnh không được để trống")
    private String imageUrl; // optional

    @NotNull(message = "Image Public ID không được để trống")
    private String imagePublicId; // optional

    @NotNull(message = "ID loại đầu nối không được để trống")
    @Positive(message = "ID loại đầu nối phải là số dương")
    private Long connectorTypeId; // optional

    @NotNull(message = "Dung lượng pin không được để trống")
    @JsonAlias({"batteryCapacity", "battery-capacity"})
    @DecimalMin(value = "0.0", inclusive = false, message = "Dung lượng pin phải lớn hơn 0")
    private Double batteryCapacityKWh; // optional
}
