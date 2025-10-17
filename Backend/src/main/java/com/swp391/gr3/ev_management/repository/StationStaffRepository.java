package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import com.swp391.gr3.ev_management.entity.StationStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationStaffRepository extends JpaRepository<StationStaff,Long> {

    // Tìm staff assignment active theo user ID
    @Query("""
              select ss from StationStaff ss
              join fetch ss.user u
              join fetch ss.station st
              where u.userId = :userId
                and ss.status = com.swp391.gr3.ev_management.enums.StaffStatus.ACTIVE
            """)
    Optional<StationStaff> findActiveByUserId(@Param("userId") Long userId);

    // Tìm tất cả staff của trạm
    List<StationStaff> findByStation_StationId(Long stationId);

    // Tìm staff active của trạm
    List<StationStaff> findByStation_StationIdAndStatus(Long stationId, String status);

    // Kiểm tra staff có active tại trạm không
    @Query("SELECT CASE WHEN COUNT(ss) > 0 THEN true ELSE false END " +
            "FROM StationStaff ss " +
            "WHERE ss.user.userId = :userId " +
            "AND ss.station.stationId = :stationId " +
            "AND ss.status = 'active'")
    boolean isStaffActiveAtStation(
            @Param("userId") Long userId,
            @Param("stationId") Long stationId
    );

    // Tìm lịch sử làm việc của staff
    @Query("SELECT ss FROM StationStaff ss " +
            "WHERE ss.user.userId = :userId " +
            "ORDER BY ss.assignedAt DESC")
    List<StationStaff> findWorkHistoryByUserId(@Param("userId") Long userId);

    // Tìm staff theo user ID và station ID
    Optional<StationStaff> findByUser_UserIdAndStation_StationId(Long userId, Long stationId);

    // Đếm số staff active của trạm
    Long countByStation_StationIdAndStatus(Long stationId, String status);

    @Query("""
              select ss from StationStaff ss
              join fetch ss.user u
              join fetch ss.station st
              where ss.stationStaffId = :stationStaffId
                and ss.status = com.swp391.gr3.ev_management.enums.StaffStatus.ACTIVE
            """)
    Optional<StationStaff> findActiveByStationStaffId(@Param("stationStaffId") Long stationStaffId);

    @Query("""
        SELECT new com.swp391.gr3.ev_management.DTO.response.StationStaffResponse(
            ss.stationStaffId,
            s.stationId,
            u.name,
            u.email,
            u.phoneNumber,
            ss.status,
            ss.assignedAt
        )
        FROM StationStaff ss
        JOIN ss.user u
        JOIN ss.station s
        WHERE u.userId = :userId
    """)
    Optional<StationStaffResponse> findByUserId(@Param("userId") Long userId);
}
