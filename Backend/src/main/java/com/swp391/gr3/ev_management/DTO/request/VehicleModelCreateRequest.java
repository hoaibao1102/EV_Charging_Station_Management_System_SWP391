package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModelCreateRequest {
    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Model is required")
    private String model;

    @Min(value = 1886, message = "Year must be realistic")
    private int year;

    @NotBlank
    private String imageUrl;

    @NotNull(message = "connectorTypeId is required")
    private Integer connectorTypeId;

//    @SuppressWarnings("unused")
//    @AssertTrue(message = "Year cannot be in the far future")
//    public boolean isYearNotInFarFuture() {
//        return year <= Year.now().getValue() + 1;
//    }
}
