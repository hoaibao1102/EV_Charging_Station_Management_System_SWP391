package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import com.swp391.gr3.ev_management.repository.StationStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffStationServiceImpl implements StaffStationService {

    private final StationStaffRepository stationStaffRepository;


    @Override
    public StationStaffResponse getStaffByUserId(Long userId) {
        return stationStaffRepository.findByUserId(userId).orElse(null);
    }
}
