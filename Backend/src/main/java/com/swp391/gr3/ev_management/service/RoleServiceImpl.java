package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Role;
import com.swp391.gr3.ev_management.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

}
