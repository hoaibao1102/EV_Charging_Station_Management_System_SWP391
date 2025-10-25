package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.enums.StaffStatus;
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

    // Tìm tất cả staff của trạm
    List<StationStaff> findByStation_StationId(Long stationId);

    List<StationStaff> findByStation_StationIdAndStaff_Status(Long stationId, StaffStatus status);

    // Kiểm tra staff có active tại trạm không (so sánh enum)
    @Query("""
        select case when count(ss) > 0 then true else false end
        from StationStaff ss
        where ss.staff.user.userId = :userId
          and ss.station.stationId = :stationId
          and ss.staff.status = com.swp391.gr3.ev_management.enums.StaffStatus.ACTIVE
    """)
    boolean isStaffActiveAtStation(@Param("userId") Long userId,
                                   @Param("stationId") Long stationId);

    // Lịch sử làm việc của staff theo userId
    @Query("""
        select ss from StationStaff ss
        where ss.staff.user.userId = :userId
        order by ss.assignedAt desc
    """)
    List<StationStaff> findWorkHistoryByUserId(@Param("userId") Long userId);

    // ❌ Sai: findByUser_UserId... — StationStaff không có field user
    // ✅ Đúng: đi qua staff.user
    Optional<StationStaff> findByStaff_User_UserIdAndStation_StationId(Long userId, Long stationId);

    // Đếm số staff theo status (nằm ở Staffs)
    Long countByStation_StationIdAndStaff_Status(Long stationId, StaffStatus status);

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
}
