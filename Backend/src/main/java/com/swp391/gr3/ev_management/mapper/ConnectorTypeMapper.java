package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.ConnectorTypeResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import org.springframework.stereotype.Component;

@Component // Đánh dấu mapper là một Spring Bean để có thể @Autowired
public class ConnectorTypeMapper {

    /**
     * Chuyển đổi từ entity ConnectorType -> DTO ConnectorTypeResponse
     */
    public ConnectorTypeResponse toResponse(ConnectorType connectorType) {
        if (connectorType == null) {
            return null;
        }

        return ConnectorTypeResponse.builder()
                .connectorTypeId(connectorType.getConnectorTypeId())
                .code(connectorType.getCode())
                .mode(connectorType.getMode())
                .displayName(connectorType.getDisplayName())
                .defaultMaxPowerKW(connectorType.getDefaultMaxPowerKW())
                .isDeprecated(connectorType.getIsDeprecated())
                .build();
    }
}
