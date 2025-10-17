package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.StaffStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StationStaffResponse {
    private Long stationStaffId;
    private Long stationId;
    private String name;
    private String email;
    private String phoneNumber;
    private StaffStatus status;
    private LocalDateTime assignedAt;
}
