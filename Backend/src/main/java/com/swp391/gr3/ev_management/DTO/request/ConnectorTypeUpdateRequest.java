package com.swp391.gr3.ev_management.DTO.request;

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
    @Size(max = 20, message = "Code không được quá 20 ký tự")
    private String code;

    @Size(max = 10, message = "Mode không được quá 10 ký tự")
    private String mode;

    @Size(max = 100, message = "Display name không được quá 100 ký tự")
    private String displayName;

    @Positive(message = "Default max power phải lớn hơn 0")
    private double defaultMaxPowerKW;

    private Boolean isDeprecated;
}
