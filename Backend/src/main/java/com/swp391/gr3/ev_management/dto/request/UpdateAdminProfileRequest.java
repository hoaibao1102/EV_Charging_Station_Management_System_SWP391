package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAdminProfileRequest {
    @NotNull(message = "Name cannot be null")
    private String phoneNumber;
}
