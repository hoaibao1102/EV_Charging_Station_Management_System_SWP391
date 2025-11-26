package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.dto.response.StationStaffResponse;
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

    // ✅ Tìm assignment (phân công staff ↔ station) đang ACTIVE theo userId.
    // - join fetch ss.staff và ss.station để load sẵn Staff & Station (tránh N+1)
    // - chỉ lấy khi Staff đang ACTIVE
    @Query("""
    select ss from StationStaff ss
    join fetch ss.staff s
    join s.user u
    join fetch ss.station st
    where u.userId = :userId
      and s.status = com.swp391.gr3.ev_management.enums.StaffStatus.ACTIVE
      and ss.unassignedAt is null
""")
    List<StationStaff> findActiveByUserId(@Param("userId") Long userId);

    // ✅ Tìm assignment theo stationStaffId (chỉ khi staff ACTIVE).
    // - Lấy chi tiết assignment + staff + station phục vụ hiển thị/kiểm tra quyền
    @Query("""
        select ss from StationStaff ss
        join fetch ss.staff s
        join s.user u
        join fetch ss.station st
        where ss.stationStaffId = :stationStaffId
          and s.status = com.swp391.gr3.ev_management.enums.StaffStatus.ACTIVE
    """)
    Optional<StationStaff> findActiveByStationStaffId(@Param("stationStaffId") Long stationStaffId);

    // ✅ Truy vấn projection trực tiếp sang DTO StationStaffResponse (nhẹ, không fetch join).
    // - Dùng khi chỉ cần dữ liệu hiển thị, không cần entity đầy đủ
    @Query("""
        select new com.swp391.gr3.ev_management.dto.response.StationStaffResponse(
            ss.stationStaffId,
            s.stationId,
            stf.staffId,
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
                    and ss.unassignedAt is null
    """)
    Optional<StationStaffResponse> findByUserId(@Param("userId") Long userId);

    // ✅ Ghi đè findById để luôn load sâu các quan hệ "staff", "staff.user", "station".
    // - @EntityGraph đảm bảo khi gọi findById(id) sẽ fetch kèm các thuộc tính chỉ định
    @EntityGraph(attributePaths = {"staff", "staff.user", "station"})
    Optional<StationStaff> findById(Long id);

    // ✅ Kiểm tra staff hiện đang có assignment ACTIVE hay chưa (unassignedAt IS NULL).
    // - Tránh gán 1 staff vào nhiều trạm cùng lúc
    boolean existsByStaff_StaffIdAndUnassignedAtIsNull(Long staffId);

    // ✅ Lấy assignment đang ACTIVE của một staff theo staffId.
    // - ACTIVE được hiểu là unassignedAt IS NULL
    @Query("""
           SELECT ss FROM StationStaff ss
           WHERE ss.staff.staffId = :staffId AND ss.unassignedAt IS NULL
           """)
    Optional<StationStaff> findActiveByStaffId(Long staffId);

    // ✅ Lấy đầy đủ entity assignment theo staffId, fetch sâu staff.user và station.
    // - Phù hợp khi cần thao tác logic với đủ quan hệ trong một query
    @Query("""
       select s from StationStaff s
       join fetch s.staff staff
       join fetch staff.user u
       join fetch s.station st
       where staff.staffId = :staffId
       """)
    Optional<StationStaff> findEntityByStaffId(Long staffId);

    // ✅ Truy vấn projection DTO theo staffId (nhẹ, chỉ dữ liệu cần thiết).
    @Query("""
    select new com.swp391.gr3.ev_management.dto.response.StationStaffResponse(
        ss.stationStaffId,
        s.stationId,
        stf.staffId,
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
            and ss.unassignedAt is null
    """)
    Optional<StationStaffResponse> findByStaffId(@Param("staffId") Long staffId);

    // ✅ Kiểm tra staff đã được gán vào chính station đó và assignment còn ACTIVE hay chưa.
    // - Dùng để ngăn tạo assignment trùng (staff, station)
    boolean existsByStaff_StaffIdAndStation_StationIdAndUnassignedAtIsNull(Long staffId, Long stationId);

    // ✅ Lấy tất cả assignment ACTIVE (unassignedAt is null) theo userId.
    // - fetch staff.user và station để hiển thị đầy đủ
    @Query("""
select s from StationStaff s
join fetch s.staff staff
join fetch staff.user u
join fetch s.station st
where u.userId = :userId
  and s.unassignedAt is null
""")
    List<StationStaff> findStationStaffByUserId(@Param("userId") Long userId);
}
