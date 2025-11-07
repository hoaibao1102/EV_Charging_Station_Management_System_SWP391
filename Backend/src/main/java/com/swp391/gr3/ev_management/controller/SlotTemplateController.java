package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.SlotTemplateResponse;
import com.swp391.gr3.ev_management.service.SlotTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/slot-templates")
@RequiredArgsConstructor
@Tag(name = "Slot Template", description = "APIs for managing slot templates")
public class SlotTemplateController {

    private final SlotTemplateService slotTemplateService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate")
    @Operation(summary = "Generate slot templates for a given configuration and date range")
    public ResponseEntity<Void> generateTemplates(
            @RequestParam Long configId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate) {
        slotTemplateService.generateDailyTemplates(configId, startDate, endDate);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all slot configurations")
    public ResponseEntity<List<SlotTemplateResponse>> getAll() {
        return ResponseEntity.ok(slotTemplateService.getAll());
    }

    @GetMapping("{templateId}")
    @Operation(summary = "Get slot templates by configuration ID", description = "Retrieve all slot templates associated with a specific configuration ID")
    public ResponseEntity<SlotTemplateResponse> getById(@PathVariable Long templateId) {
        SlotTemplateResponse response = slotTemplateService.getById(templateId);
        if (response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }
}
