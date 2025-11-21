package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.SlotTemplateResponse;
import com.swp391.gr3.ev_management.entity.SlotTemplate;

import java.time.LocalDateTime;
import java.util.List;

public interface SlotTemplateService {

    List<SlotTemplateResponse> generateDailyTemplates(Long configId, LocalDateTime forDate, LocalDateTime endDate);

    SlotTemplateResponse getById(Long templateId);

    List<SlotTemplateResponse> getAll();

    List<SlotTemplate> findByConfig_ConfigIdAndStartTimeBetween(Long configId, LocalDateTime start, LocalDateTime end);

    List<SlotTemplate> findAllById(List<Long> templateIds);
}
