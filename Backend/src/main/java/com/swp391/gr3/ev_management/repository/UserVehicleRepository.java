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
    
    // Find all vehicles by driver ID
    List<UserVehicle> findByDriver_DriverId(Long driverId);
    
    // Find vehicle by ID with model details
    @Query("SELECT v FROM UserVehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.connectorType " +
           "WHERE v.vehicleId = :vehicleId")
    Optional<UserVehicle> findByIdWithDetails(@Param("vehicleId") Long vehicleId);
    
    // Find vehicles by driver with model details
    @Query("SELECT v FROM UserVehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.connectorType " +
           "WHERE v.driver.driverId = :driverId")
    List<UserVehicle> findByDriverIdWithDetails(@Param("driverId") Long driverId);
}
