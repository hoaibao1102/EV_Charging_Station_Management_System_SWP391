package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.*;
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

    @NotNull(message = "Email cannot be blank")
    @Email(message = "Email is not valid")
    private String email;

    @NotNull(message = "Phone number cannot be null")
    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message="Invalid VN phone")
    private String phoneNumber;

    @NotNull(message = "Address cannot be null")
    private String address;

    @NotNull(message = "Date of birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender cannot be null")
    private String gender;
}
