package com.swp391.gr3.ev_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetUsersResponse {
    private Long userId;
    private String email;
    private String phoneNumber;
    private String name;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String status;
    private long sessionCount;
    private String roleName; // ADMIN, STAFF, DRIVER
}
