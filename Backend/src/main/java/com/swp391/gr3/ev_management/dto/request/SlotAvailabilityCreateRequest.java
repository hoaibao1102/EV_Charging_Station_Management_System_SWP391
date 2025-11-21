package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** Tạo Availability cho một (hoặc nhiều) template và một danh sách connectorTypeIds */
@Data
public class SlotAvailabilityCreateRequest {

    @NotEmpty(message = "Danh sách Template ID không được để trống")
    private List<Long> templateIds;       // cho phép tạo theo nhiều template

    @NotEmpty(message = "Danh sách Connector Type ID không được để trống")
    private List<Long> connectorTypeIds; // danh sách loại đầu nối cần tạo availability
}
