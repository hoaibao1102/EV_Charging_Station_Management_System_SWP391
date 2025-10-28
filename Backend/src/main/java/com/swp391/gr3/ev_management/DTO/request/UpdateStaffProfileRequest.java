package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateStaffProfileRequest {
    private String fullName;
    private LocalDate dateOfBirth;
    @Pattern(regexp = "^[MFO]$", message = "Giới tính không đúng định dạng")
    private String gender;
    private String address;
}
