package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverUpdateRequest {

    @NotNull(message = "Name cannot be null")
    private String name;
    @NotNull(message = "Email cannot be null")
    private String email;
    @NotNull(message = "Phone number cannot be null")
    private String phoneNumber;
    @NotNull(message = "Address cannot be null")
    private String address;
    @NotNull(message = "Date of birth cannot be null")
    private LocalDate dateOfBirth;
    @NotNull(message = "Gender cannot be null")
    private String gender;
}
