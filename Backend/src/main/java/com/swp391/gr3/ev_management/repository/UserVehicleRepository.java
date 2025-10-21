package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVehicleRepository extends JpaRepository<UserVehicle,Long> {
    
    // BR-03: Check if license plate already exists
    boolean existsByVehiclePlate(String licensePlate);
    
    // Count vehicles by model ID (moved from VehicleRepisitory)
    long countByModel_ModelId(Long modelId);
    
    // Find vehicle by ID with model details
    // JOIN FETCH (not LEFT) vì model & connectorType đều nullable=false
    @Query("SELECT v FROM UserVehicle v " +
           "JOIN FETCH v.model m " +
           "JOIN FETCH m.connectorType " +
           "WHERE v.vehicleId = :vehicleId")
    Optional<UserVehicle> findByIdWithDetails(@Param("vehicleId") Long vehicleId);
    
    // Find vehicles by driver with model details
    @Query("SELECT DISTINCT v FROM UserVehicle v " +
           "JOIN FETCH v.model m " +
           "JOIN FETCH m.connectorType " +
           "WHERE v.driver.driverId = :driverId")
    List<UserVehicle> findByDriverIdWithDetails(@Param("driverId") Long driverId);
}
