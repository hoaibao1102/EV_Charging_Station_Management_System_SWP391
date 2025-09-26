package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Integer> {
    public Users findUsersByPhoneNumberAndPassword(String phoneNumber , String password);
    public Users findUsersByPhoneNumber(String phoneNumber);
    public Users findUserByUserId(Long userId);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByEmail(String email);
}
