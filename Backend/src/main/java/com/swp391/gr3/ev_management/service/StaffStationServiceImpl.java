package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.ChargingStationRepository;
import com.swp391.gr3.ev_management.repository.StationStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StaffStationServiceImpl implements StaffStationService {

    private final StationStaffRepository stationStaffRepository;
    private final ChargingStationRepository chargingStationRepository;

    @Override
    public StationStaffResponse getStaffByUserId(Long userId) {
        return stationStaffRepository.findByUserId(userId).orElse(null);
    }

    /** ✅ Cập nhật station cho staff theo staffId + stationId */
    @Transactional
    @Override
    public StationStaffResponse updateStation(Long staffId, Long stationId) {
        StationStaff ss = stationStaffRepository.findEntityByStaffId(staffId)
                .orElseThrow(() -> new ErrorException("Staff not found with staffId " + staffId));

        var newStation = chargingStationRepository.findById(stationId)
                .orElseThrow(() -> new ErrorException("Station not found with id " + stationId));

        // Nếu trạm không đổi thì trả luôn
        if (ss.getStation() != null && ss.getStation().getStationId().equals(stationId)) {
            return stationStaffRepository.findByStaffId(staffId)
                    .orElseThrow(() -> new ErrorException("Failed to load staff after update"));
        }

        // (Tuỳ chọn) cập nhật mốc thời gian
        ss.setStation(newStation);
        ss.setAssignedAt(LocalDateTime.now());
        ss.setUnassignedAt(LocalDateTime.now()); // nếu bạn log thời điểm rời trạm cũ, hãy set trước khi đổi

        stationStaffRepository.save(ss);

        // Trả projection response giống getStaffByUserId
        return stationStaffRepository.findByStaffId(staffId)
                .orElseThrow(() -> new ErrorException("Failed to load staff after update"));
    }
}
