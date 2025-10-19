package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DriverResponse {
    private Long driverId;
    private Long userId;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private String gender;              // Giới tính: M/F/O
    private LocalDate dateOfBirth;      // Ngày sinh
    private DriverStatus status;        // Trạng thái: ACTIVE/BANNED/SUSPENDED
    private LocalDateTime createdAt;    // Ngày tạo tài khoản
    private LocalDateTime updatedAt;
}
