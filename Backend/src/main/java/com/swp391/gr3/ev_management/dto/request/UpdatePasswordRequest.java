package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {

    @NotNull(message = "Mật khẩu cũ không được để trống")
    @Size(min = 6, message = "Mật khẩu cũ phải có ít nhất 6 ký tự")
    private String oldPassword;

    @NotNull(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword;

    @NotNull(message = "Xác nhận mật khẩu không được để trống")
    @Size(min = 6, message = "Xác nhận mật khẩu phải có ít nhất 6 ký tự")
    private String confirmPassword;
}
