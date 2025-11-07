package com.swp391.gr3.ev_management.dto.request;

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
    @NotNull(message = "Code không được để trống")
    @Size(max = 20, message = "Code không được quá 20 ký tự")
    private String code;

    @NotNull(message = "Mode không được để trống")
    @Size(max = 10, message = "Mode không được quá 10 ký tự")
    private String mode;

    @NotNull(message = "Display name không được để trống")
    @Size(max = 100, message = "Display name không được quá 100 ký tự")
    private String displayName;

    @NotNull(message = "Default max power không được để trống")
    @Positive(message = "Default max power phải lớn hơn 0")
    private double defaultMaxPowerKW;

    private Boolean isDeprecated;
}
