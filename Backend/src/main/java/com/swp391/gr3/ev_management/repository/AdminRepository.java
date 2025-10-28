package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    boolean existsByUser_UserId(Long userUserId);

    @Query("select a from Admin a join fetch a.user u where u.userId = :userId")
    Optional<Admin> findByUserIdWithUser(Long userId);
}