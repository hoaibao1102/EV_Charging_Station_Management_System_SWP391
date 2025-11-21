package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverUpdateRequest {

    @NotNull(message = "Tên không được để trống")
    private String name;

    @NotNull(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotNull(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Số điện thoại Việt Nam không hợp lệ")
    private String phoneNumber;

    @NotNull(message = "Địa chỉ không được để trống")
    private String address;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    @Pattern(regexp = "^[MF]$", message = "Giới tính phải là 'M' hoặc 'F'")
    private String gender;
}
