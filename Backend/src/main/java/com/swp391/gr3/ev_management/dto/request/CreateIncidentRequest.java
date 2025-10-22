package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

@Data
public class CreateIncidentRequest {
    private Long StaffId;
    private String title;
    private String description;
    private String severity;
}
