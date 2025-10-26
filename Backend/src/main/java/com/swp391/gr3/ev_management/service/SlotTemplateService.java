package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.SlotTemplateResponse;
import com.swp391.gr3.ev_management.entity.SlotTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SlotTemplateService {
    List<SlotTemplateResponse> generateDailyTemplates(Long configId, LocalDateTime forDate);
    List<SlotTemplateResponse> generateTemplatesForRange(Long configId, LocalDateTime startDate, LocalDateTime endDate);
    List<SlotTemplateResponse> generateTemplatesFromConfig(Long configId);
    SlotTemplateResponse getById(Long templateId);
    List<SlotTemplateResponse> getAll();
}
