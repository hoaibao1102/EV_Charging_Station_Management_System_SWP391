package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

@Data
public class UpdatePasswordRequest {
    private String oldPassword;
    private String newPassword;
}
