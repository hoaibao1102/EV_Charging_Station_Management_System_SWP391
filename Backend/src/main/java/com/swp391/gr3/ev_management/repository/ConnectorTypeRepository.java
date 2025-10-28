package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ConnectorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectorTypeRepository extends JpaRepository<ConnectorType, Long> {
    boolean existsByCodeAndMode(String code, String mode);
    ConnectorType findByCode(String code);
    List<ConnectorType> findDistinctByChargingPoints_Station_StationId(Long stationId);
}
