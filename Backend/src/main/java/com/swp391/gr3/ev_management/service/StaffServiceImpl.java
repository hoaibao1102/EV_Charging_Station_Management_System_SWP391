package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.DTO.response.StaffResponse;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.Staffs;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.mapper.StaffMapper;
import com.swp391.gr3.ev_management.repository.StaffsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements  StaffService {

    private final StaffsRepository staffsRepository;

    private final StaffMapper staffMapper;

    @Override
    public Staffs findByStaffId(Long staffId) {
        return staffsRepository.findByStaffId(staffId);
    }

    @Override
    @Transactional
    public StaffResponse updateStatus(Long userId, StaffStatus status) {
        Staffs staffs = staffsRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));
        staffs.setStatus(status);
        staffsRepository.save(staffs);
        return staffMapper.toStaffResponse(staffs);
    }

}
