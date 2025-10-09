package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    public Role findByRoleName(String roleName);
    public Role findByRoleId(Long roleId);

}
