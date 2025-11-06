package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateReportRequest {
    @NotNull(message = "Title cannot be null")
    private String title;
    @NotNull(message = "Description cannot be null")
    private String description;
    @NotNull(message = "Severity cannot be null")
    private String severity;
}
