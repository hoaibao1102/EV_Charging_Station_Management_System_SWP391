package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepisitory extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findDriverByDriverId(Long DriverId);
    boolean existsByLisencePlate(String lisencePlate);
}
