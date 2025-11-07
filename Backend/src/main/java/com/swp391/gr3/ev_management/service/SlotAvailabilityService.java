package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.SlotAvailabilityCreateRequest;
import com.swp391.gr3.ev_management.dto.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.enums.SlotStatus;

import java.time.LocalDate;
import java.util.List;

public interface SlotAvailabilityService {

    /** Tạo availability cho danh sách template + connector types chỉ định */
    List<SlotAvailabilityResponse> createForTemplates(SlotAvailabilityCreateRequest req);

    /** Tạo availability cho TẤT CẢ templates trong ngày (của 1 config) và toàn bộ connector types của station */
    List<SlotAvailabilityResponse> createForConfigInDate(Long configId, LocalDate date);

    /** Cập nhật trạng thái 1 slot availability */
    SlotAvailabilityResponse updateStatus(Long slotAvailabilityId, SlotStatus status);

    List<SlotAvailabilityResponse>  findByPointId(Long pointId);

    List<SlotAvailabilityResponse> findAll();
}
