package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAdminProfileRequest {
    @NotNull(message = "Name cannot be null")
    private String phoneNumber;
}
