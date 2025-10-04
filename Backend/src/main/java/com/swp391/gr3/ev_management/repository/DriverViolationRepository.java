package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.DriverViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverViolationRepository extends JpaRepository<DriverViolation, Long> {
    List<DriverViolation> findByDriver_DriverId(Long driverId);
}