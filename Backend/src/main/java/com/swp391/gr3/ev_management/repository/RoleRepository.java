package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Roles, Integer> {
    public Roles findByRoleName(String roleName);
    public  Roles findByRoleId(Long roleId);

}
