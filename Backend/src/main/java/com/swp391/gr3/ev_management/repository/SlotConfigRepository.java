package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotConfigRepository extends JpaRepository<SlotConfig, Long> {
    SlotConfig findByConfigId(Long slotConfigId);
    SlotConfig findByStation_StationId(Long stationId);
    List<SlotConfig> findByIsActive(SlotConfigStatus isActive);

    // Chỉ kiểm tra tồn tại ACTIVE
    @Query("SELECT COUNT(c) > 0 FROM SlotConfig c WHERE c.station.stationId = :stationId AND c.isActive = :status")
    boolean existsActiveConfig(@Param("stationId") Long stationId, @Param("status") SlotConfigStatus status);

    // Deactivate theo batch (hiệu năng & tránh non-unique)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SlotConfig c SET c.isActive = 'INACTIVE', c.activeExpire = :now " +
            "WHERE c.station.stationId = :stationId AND c.isActive = 'ACTIVE'")
    int deactivateActiveByStation(@Param("stationId") Long stationId, @Param("now") LocalDateTime now);
}
