package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident,Long> {
    // Tìm tất cả incident của trạm
    @Query("SELECT i FROM Incident i " +
            "WHERE i.stationStaff.station.stationId = :stationId " +
            "ORDER BY i.reportedAt DESC")
    List<Incident> findByStationId(@Param("stationId") Long stationId);

    // Tìm incident theo staff ID
    List<Incident> findByStationStaff_StationStaffId(Long stationStaffId);

    // Tìm incident theo status
    List<Incident> findByStatus(String status);

    // Tìm incident theo severity
    List<Incident> findBySeverity(String severity);

    // Tìm incident chưa giải quyết của trạm
    @Query("SELECT i FROM Incident i " +
            "WHERE i.stationStaff.station.stationId = :stationId " +
            "AND i.status IN ('Reported', 'In Progress') " +
            "ORDER BY i.severity DESC, i.reportedAt ASC")
    List<Incident> findUnresolvedByStationId(@Param("stationId") Long stationId);

    // Tìm incident critical chưa giải quyết
    @Query("SELECT i FROM Incident i " +
            "WHERE i.severity = 'Critical' " +
            "AND i.status IN ('Reported', 'In Progress') " +
            "ORDER BY i.reportedAt ASC")
    List<Incident> findCriticalUnresolvedIncidents();

    // Tìm incident trong khoảng thời gian
    @Query("SELECT i FROM Incident i " +
            "WHERE i.stationStaff.station.stationId = :stationId " +
            "AND i.reportedAt >= :startDate " +
            "AND i.reportedAt <= :endDate " +
            "ORDER BY i.reportedAt DESC")
    List<Incident> findByStationAndDateRange(
            @Param("stationId") Long stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Đếm incident chưa giải quyết
    @Query("SELECT COUNT(i) FROM Incident i " +
            "WHERE i.stationStaff.station.stationId = :stationId " +
            "AND i.status IN ('Reported', 'In Progress')")
    Long countUnresolvedByStationId(@Param("stationId") Long stationId);

    // Đếm incident theo severity
    Long countByStationStaff_Station_StationIdAndSeverity(Long stationId, String severity);
}
