package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.emuns.DriverStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverUpdateRequest {

    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private DriverStatus driverStatus; // optional - chỉ admin mới update
}
