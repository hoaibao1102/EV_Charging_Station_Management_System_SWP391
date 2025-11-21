package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectorTypeUpdateRequest {

    @NotBlank(message = "Mã code không được để trống")
    @Size(max = 20, message = "Mã code không được vượt quá 20 ký tự")
    private String code;

    @NotBlank(message = "Chế độ không được để trống")
    @Size(max = 10, message = "Chế độ không được vượt quá 10 ký tự")
    private String mode;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 100, message = "Tên hiển thị không được vượt quá 100 ký tự")
    private String displayName;

    @NotNull(message = "Công suất tối đa mặc định là bắt buộc")
    @Positive(message = "Công suất tối đa mặc định phải lớn hơn 0")
    private Double defaultMaxPowerKW;

    private Boolean isDeprecated;
}
