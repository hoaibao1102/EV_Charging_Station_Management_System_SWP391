package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.DTO.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.entity.SlotAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotAvailabilityRepository extends JpaRepository<SlotAvailability, Long> {

    List<SlotAvailability> findByTemplate_TemplateId(Long templateId);

    // lấy các slot availability theo config và trong ngày (cửa sổ [start, end))
    List<SlotAvailability> findByTemplate_Config_ConfigIdAndDateBetween(
            Long configId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    boolean existsByTemplate_TemplateIdAndChargingPoint_PointIdAndDate(Long templateId, Long pointId, LocalDateTime date);
    void deleteByTemplate_Config_ConfigIdAndDateBetween(Long configId, LocalDateTime start, LocalDateTime end);


    List<SlotAvailability> findAllByChargingPoint_PointId(Long pointId);
}
