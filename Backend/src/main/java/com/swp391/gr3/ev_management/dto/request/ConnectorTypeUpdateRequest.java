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

    @NotNull(message = "Code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    private String code;

    @NotNull(message = "Mode is required")
    @Size(max = 10, message = "Mode must not exceed 10 characters")
    private String mode;

    @NotNull(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @NotNull(message = "Default max power is required")
    @Positive(message = "Default max power must be greater than 0")
    private double defaultMaxPowerKW;

    private Boolean isDeprecated;
}
