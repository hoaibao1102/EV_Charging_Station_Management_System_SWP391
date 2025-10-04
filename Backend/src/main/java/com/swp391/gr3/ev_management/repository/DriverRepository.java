package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    
    @Query("SELECT d FROM Driver d JOIN FETCH d.user WHERE d.driverId = :driverId")
    Optional<Driver> findByIdWithUser(Long driverId);
    
    boolean existsByUser_UserId(Long userId);
}
