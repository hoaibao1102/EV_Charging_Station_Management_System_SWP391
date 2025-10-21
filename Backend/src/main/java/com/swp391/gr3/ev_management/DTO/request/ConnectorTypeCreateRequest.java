package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConnectorTypeCreateRequest {

    @NotBlank(message = "Code không được để trống")
    @Size(max = 20, message = "Code không được quá 20 ký tự")
    private String code;

    @NotBlank(message = "Mode không được để trống")
    @Size(max = 10, message = "Mode không được quá 10 ký tự")
    private String mode;

    @NotBlank(message = "Display name không được để trống")
    @Size(max = 100, message = "Display name không được quá 100 ký tự")
    private String displayName;

    @NotNull(message = "Default max power không được để trống")
    @Positive(message = "Default max power phải lớn hơn 0")
    private double defaultMaxPowerKW;

    @NotNull(message = "IsDeprecated không được để trống")
    private Boolean isDeprecated;

}
