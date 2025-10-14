package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.emuns.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private DriverStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
