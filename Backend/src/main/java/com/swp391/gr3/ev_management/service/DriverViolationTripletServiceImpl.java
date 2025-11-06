package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.DriverViolationTripletResponse;
import com.swp391.gr3.ev_management.enums.TripletStatus;
import com.swp391.gr3.ev_management.mapper.DriverViolationTripletMapper;
import com.swp391.gr3.ev_management.repository.DriverViolationTripletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverViolationTripletServiceImpl implements DriverViolationTripletService {

    private final DriverViolationTripletRepository tripletRepository;
    private final DriverViolationTripletMapper tripletMapper;

    @Override
    public List<DriverViolationTripletResponse> getAllTriplets() {
        return tripletRepository.findAllWithDriverAndUser()
                .stream()
                .map(tripletMapper::toResponse)
                .toList();
    }

    @Override
    public List<DriverViolationTripletResponse> getTripletsByUserPhone(String phoneNumber) {
        return tripletRepository.findByUserPhoneNumber(phoneNumber)
                .stream()
                .map(tripletMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional // cần readOnly = false để update DB
    public DriverViolationTripletResponse updateTripletStatusToPaid(Long tripletId) {
        var triplet = tripletRepository.findById(tripletId)
                .orElseThrow(() -> new RuntimeException("Triplet not found with id: " + tripletId));

        // Cập nhật trạng thái
        triplet.setStatus(TripletStatus.PAID);
        triplet.setClosedAt(java.time.LocalDateTime.now());

        tripletRepository.save(triplet);

        return tripletMapper.toResponse(triplet);
    }

    @Override
    @Transactional // cần readOnly = false để update DB
    public DriverViolationTripletResponse updateTripletStatusToCanceled(Long tripletId) {
        var triplet = tripletRepository.findById(tripletId)
                .orElseThrow(() -> new RuntimeException("Triplet not found with id: " + tripletId));

        // Cập nhật trạng thái
        triplet.setStatus(TripletStatus.CANCELED);
        triplet.setClosedAt(java.time.LocalDateTime.now());

        tripletRepository.save(triplet);

        return tripletMapper.toResponse(triplet);
    }
}
