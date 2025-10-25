package com.swp391.gr3.ev_management.DTO.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffAssignResponse {
    private Long userId;
    private Long staffId;
    private Long stationStaffId;
    private Long stationId;
    private String staffStatus; // ACTIVE,...
}
