package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateReportRequest {

    @NotBlank(message = "Title cannot be null")
    private String title;

    @NotBlank(message = "Description cannot be null")
    private String description;

    @NotBlank(message = "Severity cannot be null")
    private String severity;
}
