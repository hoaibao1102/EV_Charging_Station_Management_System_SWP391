package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotConfigRepository extends JpaRepository<SlotConfig, Long> {
    SlotConfig findByConfigId(Long slotConfigId);
    SlotConfig findByStation_StationId(Long stationId);
    List<SlotConfig> findByIsActive(SlotConfigStatus isActive);


}
