package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateStaffProfileRequest {

    @NotNull(message = "Họ và tên không được để trống")
    private String fullName;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    @Pattern(regexp = "^[MF]$", message = "Giới tính phải là 'M', 'F'")
    private String gender;

    @NotNull(message = "Địa chỉ không được để trống")
    private String address;
}
