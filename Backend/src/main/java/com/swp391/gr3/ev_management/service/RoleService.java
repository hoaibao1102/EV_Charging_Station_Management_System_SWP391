package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.RegisterRequest;
import com.swp391.gr3.ev_management.entity.Roles;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface RoleService {
    List<Roles> findAll();
    public boolean addRole(Roles Role);
    public Roles findByRoleName(String roleName);

}
