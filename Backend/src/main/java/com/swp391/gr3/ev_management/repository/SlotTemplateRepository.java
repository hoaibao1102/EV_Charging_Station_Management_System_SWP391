package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.SlotTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotTemplateRepository extends JpaRepository<SlotTemplate, Long> {

    List<SlotTemplate> findByConfig_ConfigIdAndStartTimeBetween(
            Long configId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    void deleteByConfig_ConfigIdAndStartTimeBetween(
            Long configId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

}
