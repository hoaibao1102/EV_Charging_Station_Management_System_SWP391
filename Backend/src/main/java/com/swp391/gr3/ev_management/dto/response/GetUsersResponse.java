package com.swp391.gr3.ev_management.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUsersResponse {
    private String email;
    private String phoneNumber;
    private String name;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String roleName; // ADMIN, STAFF, EV_DRIVER
}
