package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public User findUsersByPhoneNumberAndPasswordHash(String phoneNumber , String password);
    public User findUsersByPhoneNumber(String phoneNumber);
    public User findUserByUserId(Long userId);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByEmail(String email);
    public List<User> findAll();
}
