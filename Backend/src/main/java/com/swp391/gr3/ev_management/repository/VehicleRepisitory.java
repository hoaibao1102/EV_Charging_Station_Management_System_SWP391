package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface VehicleRepisitory extends JpaRepository<UserVehicle, Long> {

    long countByModel_ModelId(Long modelId);
}
