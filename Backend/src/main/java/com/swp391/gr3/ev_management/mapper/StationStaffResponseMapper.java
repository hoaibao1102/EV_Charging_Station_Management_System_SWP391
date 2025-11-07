package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.StationStaffResponse;
import com.swp391.gr3.ev_management.entity.StationStaff;
import org.springframework.stereotype.Component;

@Component
public class StationStaffResponseMapper {

    /**
     * Map entity StationStaff sang DTO StationStaffResponse.
     */
    public StationStaffResponse mapToResponse(StationStaff s) {
        if (s == null) return null;
        return new StationStaffResponse(
                s.getStationStaffId(),
                s.getStaff().getStaffId(),
                s.getStation() != null ? s.getStation().getStationId() : null,
                s.getStaff().getUser().getName(),
                s.getStaff().getUser().getEmail(),
                s.getStaff().getUser().getPhoneNumber(),
                s.getStaff().getStatus(),
                s.getAssignedAt()
        );
    }
}
