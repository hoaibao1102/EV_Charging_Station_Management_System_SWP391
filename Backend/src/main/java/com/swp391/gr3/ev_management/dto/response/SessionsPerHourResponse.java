package com.swp391.gr3.ev_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionsPerHourResponse {
    private String hour;   // "00:00", "04:00", ...
    private Long count;    // số phiên sạc trong khung giờ
}
