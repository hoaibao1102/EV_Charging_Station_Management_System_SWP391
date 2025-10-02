package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargingStationRepository extends JpaRepository<ChargingStation,Long> {
    public List<ChargingStation> findByLocationContaining(Double latitude, Double longitude);
    public List<ChargingStation> findByStatus(String status);
}
