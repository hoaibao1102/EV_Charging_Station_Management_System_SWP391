package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

import java.util.Date;

@Data
public class RegisterRequest {
    private String email;
    private String phoneNumber;
    private String password;
    private String firstName;
    private String lastName;
    private Date dateOfBirth; // "2001-09-26"
    private String gender;         // có thể dùng enum
    private String address;
}


