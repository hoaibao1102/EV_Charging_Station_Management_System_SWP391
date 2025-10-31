package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Tạo Availability cho 1 (hoặc nhiều) template và 1 danh sách connectorTypeIds */
@Data
public class SlotAvailabilityCreateRequest {
    @NotNull(message = "Template IDs cannot be null")
    private List<Long> templateIds;       // cho phép tạo theo nhiều template
    @NotNull(message = "Connector Type IDs cannot be null")
    private List<Long> connectorTypeIds; // danh sách connector types muốn tạo availability
}
