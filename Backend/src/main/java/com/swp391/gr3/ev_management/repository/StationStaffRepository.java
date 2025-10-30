package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import com.swp391.gr3.ev_management.entity.StationStaff;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationStaffRepository extends JpaRepository<StationStaff, Long> {

    // Tìm assignment active theo user ID
    // (fetch staff + station, KHÔNG fetch "cháu" staff.user)
    @Query("""
        select ss from StationStaff ss
        join fetch ss.staff s
        join s.user u
        join fetch ss.station st
        where u.userId = :userId
          and s.status = com.swp391.gr3.ev_management.enums.StaffStatus.ACTIVE
    """)
    Optional<StationStaff> findActiveByUserId(@Param("userId") Long userId);

    // Tìm assignment theo id (active)
    @Query("""
        select ss from StationStaff ss
        join fetch ss.staff s
        join s.user u
        join fetch ss.station st
        where ss.stationStaffId = :stationStaffId
          and s.status = com.swp391.gr3.ev_management.enums.StaffStatus.ACTIVE
    """)
    Optional<StationStaff> findActiveByStationStaffId(@Param("stationStaffId") Long stationStaffId);

    // Projection DTO — KHÔNG dùng fetch join nên OK
    @Query("""
        select new com.swp391.gr3.ev_management.DTO.response.StationStaffResponse(
            ss.stationStaffId,
            stf.staffId,    
            s.stationId,
            u.name,
            u.email,
            u.phoneNumber,
            ss.staff.status,
            ss.assignedAt
        )
        from StationStaff ss
        join ss.staff stf
        join stf.user u
        join ss.station s
        where u.userId = :userId
    """)
    Optional<StationStaffResponse> findByUserId(@Param("userId") Long userId);

    // (Tuỳ chọn) Nếu muốn luôn load sâu staff.user + station mà không vi phạm fetch-join:
    @EntityGraph(attributePaths = {"staff", "staff.user", "station"})
    Optional<StationStaff> findById(Long id);

    boolean existsByStaff_StaffIdAndUnassignedAtIsNull(Long staffId);

    // Tìm assignment đang active (unassignedAt IS NULL) cho 1 staff
    @Query("""
           SELECT ss FROM StationStaff ss
           WHERE ss.staff.staffId = :staffId AND ss.unassignedAt IS NULL
           """)
    Optional<StationStaff> findActiveByStaffId(Long staffId);

    @Query("""
       select s from StationStaff s
       join fetch s.staff staff
       join fetch staff.user u
       join fetch s.station st
       where staff.staffId = :staffId
       """)
    Optional<StationStaff> findEntityByStaffId(Long staffId);

    @Query("""
    select new com.swp391.gr3.ev_management.DTO.response.StationStaffResponse(
        ss.stationStaffId,
        stf.staffId,
        s.stationId,
        u.name,
        u.email,
        u.phoneNumber,
        stf.status,
        ss.assignedAt
    )
    from StationStaff ss
    join ss.staff stf
    join stf.user u
    join ss.station s
    where stf.staffId = :staffId
    """)
    Optional<StationStaffResponse> findByStaffId(@Param("staffId") Long staffId);

    boolean existsByStaff_StaffIdAndStation_StationIdAndUnassignedAtIsNull(Long staffId, Long stationId);
}
