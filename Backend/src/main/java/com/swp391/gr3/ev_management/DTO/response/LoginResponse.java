package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String name;
    private String phone;
    private String email;
    private String gender;
    private String roleName;
}
