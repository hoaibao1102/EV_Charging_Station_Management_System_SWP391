package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepisitory extends JpaRepository<UserVehicle, Long> {
    List<UserVehicle> findDriverByDriver(Driver driver);
}


