package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.SlotTemplateResponse;

import java.time.LocalDate;
import java.util.List;

public interface SlotTemplateService {
    List<SlotTemplateResponse> generateDailyTemplates(Long configId, LocalDate forDate);
    List<SlotTemplateResponse> generateTemplatesForRange(Long configId, LocalDate startDate, LocalDate endDate);

    SlotTemplateResponse getById(Long templateId);
}
