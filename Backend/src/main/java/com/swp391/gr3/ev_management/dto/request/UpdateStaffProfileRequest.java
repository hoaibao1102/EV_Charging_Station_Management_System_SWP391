package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateStaffProfileRequest {
    @NotNull(message = "Full name cannot be null")
    private String fullName;
    @NotNull(message = "Date of birth cannot be null")
    private LocalDate dateOfBirth;
    @NotNull(message = "Gender cannot be null")
    @Pattern(regexp = "^[MFO]$", message = "Giới tính không đúng định dạng")
    private String gender;
    @NotNull(message = "Address cannot be null")
    private String address;
}
