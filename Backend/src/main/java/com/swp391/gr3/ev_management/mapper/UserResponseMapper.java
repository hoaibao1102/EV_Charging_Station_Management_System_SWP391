package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.GetUsersResponse;
import com.swp391.gr3.ev_management.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    /** Map User + sessionCount -> GetUsersResponse */
    public GetUsersResponse toGetUsersResponse(User user, long sessionCount) {
        if (user == null) return null;
        return GetUsersResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .address(user.getAddress())
                .status(extractStatus(user))
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .sessionCount(sessionCount)
                .build();
    }

    /* ---- helpers ---- */
    private String extractStatus(User user) {
        if (user.getStaffs() != null && user.getStaffs().getStatus() != null)
            return user.getStaffs().getStatus().name();
        if (user.getDriver() != null && user.getDriver().getStatus() != null)
            return user.getDriver().getStatus().name();
        return "UNKNOWN";
    }
}
