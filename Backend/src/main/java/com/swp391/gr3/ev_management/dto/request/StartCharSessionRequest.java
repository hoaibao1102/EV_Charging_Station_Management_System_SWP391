package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

@Data
public class StartCharSessionRequest {
    private Long staffId;
    private Long bookingId;
}
