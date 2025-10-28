package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserVehicleRepository extends JpaRepository<UserVehicle,Long> {
    
    // Find vehicles by driver with model details
    @Query("SELECT v FROM UserVehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.connectorType " +
           "WHERE v.driver.driverId = :driverId")
    List<UserVehicle> findByDriverIdWithDetails(@Param("driverId") Long driverId);

    @Query("""
           SELECT uv FROM UserVehicle uv
           JOIN FETCH uv.model m
           JOIN FETCH m.connectorType ct
           WHERE uv.driver.driverId = :driverId
           """)
    List<UserVehicle> findByDriverIdWithModelAndConnector(@Param("driverId") Long driverId);
}
