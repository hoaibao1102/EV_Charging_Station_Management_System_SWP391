package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Staffs;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffsRepository extends JpaRepository<Staffs, Long> {
    @Query("""
           select s from Staffs s
           join fetch s.user u 
           where u.userId = :userId
           """)
    Optional<Staffs> findByUserIdWithUser(@Param("userId") Long userId);

    Optional<Staffs> findByUser_UserId(Long userId);

    long countByStatus(StaffStatus status);
}
