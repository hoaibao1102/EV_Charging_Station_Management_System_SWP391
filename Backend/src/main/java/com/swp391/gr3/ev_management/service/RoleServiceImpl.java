package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Role;
import com.swp391.gr3.ev_management.repository.RoleRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private RoleRepository roleRepository;


    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository,UserRepository userRepository) {
        this.roleRepository = roleRepository;
    }


    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    public boolean addRole(Role roles) {
        return roleRepository.save(roles) != null;
    }

    @Override
    public Role findByRoleName(String roleName) {
        return roleRepository.findByRoleName(roleName);
    }

}
