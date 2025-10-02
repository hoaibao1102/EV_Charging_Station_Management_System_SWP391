package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    public boolean existsByModelName(String modelName);
}
