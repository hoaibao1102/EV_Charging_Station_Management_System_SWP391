package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Tạo Availability cho 1 (hoặc nhiều) template và 1 danh sách connectorTypeIds */
@Data
public class SlotAvailabilityCreateRequest {

    @NotEmpty(message = "Template IDs cannot be empty")
    private List<Long> templateIds;       // cho phép tạo theo nhiều template

    @NotEmpty(message = "Connector Type IDs cannot be empty")
    private List<Long> connectorTypeIds; // danh sách connector types muốn tạo availability
}
