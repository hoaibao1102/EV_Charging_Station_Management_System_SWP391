package com.swp391.gr3.ev_management.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectorTypeResponse {
    private Long connectorTypeId;
    private String code;
    private String mode;
    private String displayName;
    private double defaultMaxPowerKW;
    private Boolean isDeprecated;
}

