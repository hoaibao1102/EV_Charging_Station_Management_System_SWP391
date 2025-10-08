package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ChargingPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingPointRepository extends JpaRepository<ChargingPoint,Long> {
    // Tìm tất cả charging point của trạm
    List<ChargingPoint> findByStation_StationId(Long stationId);

    // Tìm charging point theo trạm và status
    List<ChargingPoint> findByStation_StationIdAndStatus(Long stationId, String status);

    // Tìm charging point available theo connector type
    @Query("SELECT cp FROM ChargingPoint cp " +
            "WHERE cp.station.stationId = :stationId " +
            "AND cp.connectorType.connectorTypeId = :connectorTypeId " +
            "AND cp.status = 'available'")
    List<ChargingPoint> findAvailablePointsByStationAndConnector(
            @Param("stationId") Long stationId,
            @Param("connectorTypeId") Long connectorTypeId
    );

    // Tìm charging point theo QR code (khi staff scan QR)
    Optional<ChargingPoint> findByQrCode(String qrCode);

    // Tìm charging point theo serial number
    Optional<ChargingPoint> findBySerialNumber(String serialNumber);

    // Tìm charging point theo point number và station
    Optional<ChargingPoint> findByStation_StationIdAndPointNumber(Long stationId, String pointNumber);

    // Đếm point available của trạm
    Long countByStation_StationIdAndStatus(Long stationId, String status);

    // Đếm tổng số point của trạm
    Long countByStation_StationId(Long stationId);

    // Tìm point cần bảo trì (lastMaintenanceDate > 6 tháng)
    @Query("SELECT cp FROM ChargingPoint cp " +
            "WHERE cp.station.stationId = :stationId " +
            "AND cp.lastMaintenanceDate < :sixMonthsAgo")
    List<ChargingPoint> findPointsNeedingMaintenance(
            @Param("stationId") Long stationId,
            @Param("sixMonthsAgo") java.sql.Date sixMonthsAgo
    );
}
