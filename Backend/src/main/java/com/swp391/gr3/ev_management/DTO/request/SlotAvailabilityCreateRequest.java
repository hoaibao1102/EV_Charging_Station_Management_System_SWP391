package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

import java.util.List;

/** Tạo Availability cho 1 (hoặc nhiều) template và 1 danh sách connectorTypeIds */
@Data
public class SlotAvailabilityCreateRequest {
    private List<Long> templateIds;       // cho phép tạo theo nhiều template
    private List<Long> connectorTypeIds; // danh sách connector types muốn tạo availability
}
