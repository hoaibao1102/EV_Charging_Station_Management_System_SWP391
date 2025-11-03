// src/main/java/com/swp391/gr3/ev_management/DTO/response/UserTotalsResponse.java
package com.swp391.gr3.ev_management.DTO.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserTotalsResponse {
    private long totalUsers;

    private long totalDrivers;
    private long activeDrivers;

    private long totalStaffs;
    private long activeStaffs;
}
