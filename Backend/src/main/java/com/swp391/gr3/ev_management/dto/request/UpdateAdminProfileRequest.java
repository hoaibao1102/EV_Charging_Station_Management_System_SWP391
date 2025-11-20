package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateAdminProfileRequest {

    @NotBlank(message = "Name cannot be null")
    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message="Invalid VN phone")
    private String phoneNumber;
}
