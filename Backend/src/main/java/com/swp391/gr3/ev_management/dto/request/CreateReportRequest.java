package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReportRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotBlank(message = "Mức độ nghiêm trọng không được để trống")
    private String severity;
}
