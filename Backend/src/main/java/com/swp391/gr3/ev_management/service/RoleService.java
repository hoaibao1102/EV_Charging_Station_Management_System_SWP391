package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Role;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface RoleService {
    List<Role> findAll();
    public boolean addRole(Role Role);
    public Role findByRoleName(String roleName);

}
