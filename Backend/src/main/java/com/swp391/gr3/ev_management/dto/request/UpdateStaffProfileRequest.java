package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateStaffProfileRequest {

    @NotNull(message = "Full name cannot be null")
    private String fullName;

    @NotNull(message = "Date of birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender cannot be null")
    @Pattern(regexp = "^[MFO]$", message = "Gender must be 'M', 'F', or 'O'")
    private String gender;

    @NotNull(message = "Address cannot be null")
    private String address;
}
