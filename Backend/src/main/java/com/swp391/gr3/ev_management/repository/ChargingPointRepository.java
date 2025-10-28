package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ChargingPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingPointRepository extends JpaRepository<ChargingPoint,Long> {
    // Tìm tất cả charging point của trạm
    List<ChargingPoint> findByStation_StationId(Long stationId);


    // Tìm charging point theo serial number
    Optional<ChargingPoint> findBySerialNumber(String serialNumber);

    // Tìm charging point theo point number và station
    Optional<ChargingPoint> findByStation_StationIdAndPointNumber(Long stationId, String pointNumber);

    List<ChargingPoint> findByStation_StationIdAndConnectorType_ConnectorTypeId(
            Long station_stationId, Long connectorType_connectorTypeId
    );
}
