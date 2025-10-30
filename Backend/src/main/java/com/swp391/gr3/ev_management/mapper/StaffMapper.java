package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.StaffResponse;
import com.swp391.gr3.ev_management.entity.Staffs;
import org.springframework.stereotype.Component;

@Component
public class StaffMapper {

    public StaffResponse toStaffResponse(Staffs staffs) {
        if (staffs == null) return null;

        return StaffResponse.builder()
                .staffId(staffs.getStaffId())
                .roleAtStation(staffs.getRoleAtStation() != null ? staffs.getRoleAtStation() : null)
                .status(staffs.getStatus())
                .staffName(staffs.getUser() != null ? staffs.getUser().getName() : null)
                .email(staffs.getUser() != null ? staffs.getUser().getEmail() : null)
                .userId(staffs.getUser().getUserId() != null ? staffs.getUser().getUserId() : null)
                .build();
    }
}
