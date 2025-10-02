package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    public List<Vehicle> findByUserUserId(Long userId);
}
