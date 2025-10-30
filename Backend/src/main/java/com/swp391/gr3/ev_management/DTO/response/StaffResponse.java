package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffResponse {
    private Long staffId;
    private String staffName;
    private String email;
    private String roleAtStation;
    private StaffStatus status;
    private Long userId;
}
