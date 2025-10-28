package com.swp391.gr3.ev_management.DTO.request;

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

    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
}
