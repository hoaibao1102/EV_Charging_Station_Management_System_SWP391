package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.DTO.response.SlotTemplateResponse;
import com.swp391.gr3.ev_management.service.SlotTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/slot-templates")
@RequiredArgsConstructor
@Tag(name = "Slot Template", description = "APIs for managing slot templates")
public class SlotTemplateController {

    private final SlotTemplateService slotTemplateService;

    // Tạo template cho HÔM NAY theo khung giờ activeFrom–activeExpire trong SlotConfig
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate/daily")
    @Operation(summary = "Generate slot templates for today based on the given configuration ID")
    public ResponseEntity<List<SlotTemplateResponse>> generateDaily(@RequestParam Long configId) {
        LocalDateTime today = LocalDateTime.now();
        return ResponseEntity.ok(slotTemplateService.generateDailyTemplates(configId, today));
    }

    // Tạo template cho NHIỀU ngày liên tiếp (reset sau 23:59 mỗi ngày)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate/range")
    @Operation(summary = "Generate slot templates using date range defined in config")
    public ResponseEntity<List<SlotTemplateResponse>> generateFromConfig(@RequestParam Long configId) {
        return ResponseEntity.ok(slotTemplateService.generateTemplatesFromConfig(configId));
    }

    @GetMapping("{templateId}")
    @Operation(summary = "Get slot templates by configuration ID", description = "Retrieve all slot templates associated with a specific configuration ID")
    public ResponseEntity<SlotTemplateResponse> getById(@PathVariable Long templateId) {
        SlotTemplateResponse response = slotTemplateService.getById(templateId);
        if (response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }
}
