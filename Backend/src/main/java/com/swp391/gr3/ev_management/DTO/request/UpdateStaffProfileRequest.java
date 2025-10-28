package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

@Data
public class UpdateStaffProfileRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
}
