package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.SlotTemplateResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface SlotTemplateService {
    List<SlotTemplateResponse> generateDailyTemplates(Long configId, LocalDateTime forDate, LocalDateTime endDate);
    List<SlotTemplateResponse> generateTemplatesForRange(Long configId, LocalDateTime startDate, LocalDateTime endDate);
    SlotTemplateResponse getById(Long templateId);
    List<SlotTemplateResponse> getAll();
}
